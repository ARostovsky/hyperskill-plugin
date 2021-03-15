/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.edu.learning.newproject;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.ide.RecentProjectsManager;
import com.intellij.ide.impl.TrustedProjects;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectOpenedCallback;
import com.intellij.util.PathUtil;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.ui.CCCreateCoursePreviewDialog;
import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.CourseVisibility;
import com.jetbrains.edu.learning.courseFormat.EduCourse;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils;
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector;
import com.jetbrains.edu.learning.stepik.StepikNames;
import com.jetbrains.edu.learning.stepik.StepikSolutionsLoader;
import com.jetbrains.edu.learning.stepik.StepikUser;
import com.jetbrains.edu.learning.stepik.api.StepikConnector;
import com.jetbrains.edu.learning.stepik.api.StepikCourseLoader;
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillCourseProjectGenerator;
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * If you add any new public methods here, please do not forget to add it also to
 * @see HyperskillCourseProjectGenerator
 */
public abstract class CourseProjectGenerator<S> {

  public static final Key<Boolean> EDU_PROJECT_CREATED = Key.create("edu.projectCreated");
  public static final Key<String> COURSE_MODE_TO_CREATE = Key.create("edu.courseModeToCreate");
  public static final Key<String> COURSE_TYPE_TO_CREATE = Key.create("edu.courseTypeToCreate");
  public static final Key<String> COURSE_LANGUAGE_ID_TO_CREATE = Key.create("edu.courseLanguageIdToCreate");

  private static final Logger LOG = Logger.getInstance(CourseProjectGenerator.class);

  @NotNull protected final EduCourseBuilder<S> myCourseBuilder;
  @NotNull protected Course myCourse;
  private boolean alreadyEnrolled;

  public CourseProjectGenerator(@NotNull EduCourseBuilder<S> builder, @NotNull final Course course) {
    myCourseBuilder = builder;
    myCourse = course;
  }

  public boolean beforeProjectGenerated() {
    if (!(myCourse instanceof EduCourse) || !((EduCourse)myCourse).isRemote()) return true;
    final EduCourse remoteCourse = (EduCourse) this.myCourse;
    if (remoteCourse.getId() > 0) {
      return ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
        ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);

        final StepikUser user = EduSettings.getInstance().getUser();
        if (user != null) {
          alreadyEnrolled = StepikConnector.getInstance().isEnrolledToCourse(remoteCourse.getId(), user);
          if (!alreadyEnrolled) {
            StepikConnector.getInstance().enrollToCourse(remoteCourse.getId(), user);
          }
        }
        StepikCourseLoader.loadCourseStructure(remoteCourse);
        myCourse = remoteCourse;
        return true;
      }, "Loading Course", true, null);
    }
    return true;
  }

  public void afterProjectGenerated(@NotNull Project project, @NotNull S projectSettings) {
    loadSolutions(project, myCourse);
    EduUtils.openFirstTask(myCourse, project);

    YamlFormatSynchronizer.saveAll(project);
    YamlFormatSynchronizer.startSynchronization(project);
  }

  /**
   * Generate new project and create course structure for created project
   *
   * @param location location of new course project
   * @param projectSettings new project settings
   */
  // 'projectSettings' must have S type but due to some reasons:
  //  * We don't know generic parameter of EduPluginConfigurator after it was gotten through extension point mechanism
  //  * Kotlin and Java do type erasure a little bit differently
  // we use Object instead of S and cast to S when it needed
  @SuppressWarnings("unchecked")
  @Nullable
  public final Project doCreateCourseProject(@NotNull String location, @NotNull Object projectSettings) {
    if (!beforeProjectGenerated()) {
      return null;
    }
    S castedProjectSettings = (S) projectSettings;
    Project createdProject = createProject(location, castedProjectSettings);
    if (createdProject == null) return null;
    afterProjectGenerated(createdProject, castedProjectSettings);
    return createdProject;
  }

  /**
   * Create new project in given location.
   * To create course structure: modules, folders, files, etc. use {@link CourseProjectGenerator#createCourseStructure(Project, Module, VirtualFile, Object)}
   *
   * @param locationString location of new project
   * @param projectSettings new project settings
   * @return project of new course or null if new project can't be created
   */
  @Nullable
  private Project createProject(@NotNull String locationString, @NotNull S projectSettings) {
    final File location = new File(FileUtil.toSystemDependentName(locationString));
    if (!location.exists() && !location.mkdirs()) {
      String message = ActionsBundle.message("action.NewDirectoryProject.cannot.create.dir", location.getAbsolutePath());
      Messages.showErrorDialog(message, ActionsBundle.message("action.NewDirectoryProject.title"));
      return null;
    }

    final VirtualFile baseDir = WriteAction.compute(() -> LocalFileSystem.getInstance().refreshAndFindFileByIoFile(location));
    if (baseDir == null) {
      LOG.error("Couldn't find '" + location + "' in VFS");
      return null;
    }
    VfsUtil.markDirtyAndRefresh(false, true, true, baseDir);

    RecentProjectsManager.getInstance().setLastProjectCreationLocation(PathUtil.toSystemIndependentName(location.getParent()));

    baseDir.putUserData(COURSE_MODE_TO_CREATE, myCourse.getCourseMode());
    baseDir.putUserData(COURSE_TYPE_TO_CREATE, myCourse.getItemType());
    baseDir.putUserData(COURSE_LANGUAGE_ID_TO_CREATE, myCourse.getLanguageID());

    ProjectOpenedCallback callback = (p, module) -> createCourseStructure(p, module, baseDir, projectSettings);
    Project project = OpenProjectUtils.openNewProject(location.toPath(), callback);
    if (project != null) {
      project.putUserData(EDU_PROJECT_CREATED, true);
    }
    return project;
  }

  /**
   * Create course structure for already created project.
   * @param project course project
   * @param module base project module
   * @param baseDir base directory of project
   * @param settings project settings
   */
  @VisibleForTesting
  public void createCourseStructure(@NotNull Project project,
                                    @NotNull Module module,
                                    @NotNull VirtualFile baseDir,
                                    @NotNull S settings) {
    GeneratorUtils.initializeCourse(project, myCourse);

    boolean isNewCourseCreatorCourse = CCUtils.isCourseCreator(project) && myCourse.getItems().isEmpty();

    if (isCourseTrusted(myCourse, isNewCourseCreatorCourse)) {
      markTrusted(project);
    }

    if (isNewCourseCreatorCourse) {
      final Lesson lesson = myCourseBuilder.createInitialLesson(project, myCourse);
      if (lesson != null) {
        myCourse.addLesson(lesson);
      }
    }

    try {
      ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
        ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        if (CCUtils.isCourseCreator(project)) {
          CCUtils.initializeCCPlaceholders(project, myCourse);
        }
        GeneratorUtils.createCourse(myCourse, baseDir, indicator);
        if (myCourse instanceof EduCourse && ((EduCourse)myCourse).isRemote() && CCUtils.isCourseCreator(project)) {
          checkIfAvailableOnRemote();
        }
        createAdditionalFiles(project, baseDir, isNewCourseCreatorCourse);
        EduCounterUsageCollector.eduProjectCreated(myCourse);

        return null; // just to use correct overloading of `runProcessWithProgressSynchronously` method
      }, "Generating Course Structure", false, project);
    } catch (IOException e) {
      LOG.error("Failed to generate course", e);
    }
  }

  private void checkIfAvailableOnRemote() {
    EduCourse courseFromStepik = StepikConnector.getInstance().getCourseInfo(myCourse.getId(), null, true);
    if (courseFromStepik == null) {
      LOG.warn("Failed to get stepik course for imported from zip course with id: " + myCourse.getId());
      LOG.info("Converting course to local. Course id: " + myCourse.getId());
      ((EduCourse)myCourse).convertToLocal();
    }
  }

  private void loadSolutions(@NotNull Project project, @NotNull Course course) {
    if (course.isStudy() && course instanceof EduCourse && ((EduCourse)course).isRemote() && EduSettings.isLoggedIn()) {
      PropertiesComponent.getInstance(project).setValue(StepikNames.ARE_SOLUTIONS_UPDATED_PROPERTY, true, false);
      if (alreadyEnrolled) {
        StepikSolutionsLoader stepikSolutionsLoader = StepikSolutionsLoader.getInstance(project);
        stepikSolutionsLoader.loadSolutionsInBackground();
        EduCounterUsageCollector.synchronizeCourse(course, EduCounterUsageCollector.SynchronizeCoursePlace.PROJECT_GENERATION);
      }
    }
  }

  /**
   * Creates additional files that are not in course object
   *
   * @param project course project
   * @param baseDir base directory of project
   * @param isNewCourse {@code true} if course is new one, {@code false} otherwise
   *
   * @throws IOException
   */
  public void createAdditionalFiles(@NotNull Project project,
                                    @NotNull VirtualFile baseDir,
                                    boolean isNewCourse) throws IOException {}

  // TODO: provide more precise heuristic for Gradle, sbt and other "dangerous" build systems
  // See https://youtrack.jetbrains.com/issue/EDU-4182
  private static boolean isCourseTrusted(@NotNull Course course, boolean isNewCourseCreatorCourse) {
    if (isNewCourseCreatorCourse) return true;
    if (!(course instanceof EduCourse)) return true;
    if (course.getVisibility() instanceof CourseVisibility.FeaturedVisibility) return true;
    if (course.getDataHolder().getUserData(CCCreateCoursePreviewDialog.IS_COURSE_PREVIEW_KEY) == Boolean.TRUE) return true;
    return false;
  }

  private static void markTrusted(@NotNull Project project) {
    // BACKCOMPAT: 2020.3. Just drop this try and inline the method.
    // Try to load `TrustedProjects` class to ensure that IDE already has the corresponding API
    // and do nothing if API doesn't exist.
    // The API is available only since 2020.3.3
    try {
      //noinspection ResultOfMethodCallIgnored
      TrustedProjects.class.getName();
    } catch (LinkageError e) {
      return;
    }

    TrustedProjects.setTrusted(project, true);
  }
}
