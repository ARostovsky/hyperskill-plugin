package org.hyperskill.academy.coursecreator.projectView;

import com.intellij.concurrency.ContextAwareRunnable;
import com.intellij.ide.TooltipTitle;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupCornerType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsContexts.Tooltip;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.openapi.util.text.Strings;
import com.intellij.reference.SoftReference;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.BrowserLink;
import com.intellij.ui.components.JBFontScaler;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.Alarm;
import com.intellij.util.ui.JBEmptyBorder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.JBValue;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.accessibility.ScreenReader;
import org.jetbrains.annotations.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static com.intellij.openapi.util.text.HtmlChunk.html;

/**
 * Copied and modified {@link com.intellij.ide.HelpTooltip} from the intellij platform
 * At the moment, we cannot use the HelpTooltip from the platform, since the platform HelpTooltip does not have the customization capabilities we need.
 * TODO() Modify this class in the platform code to use version from IntelliJ platform
 */

public class SyncChangesHelpTooltip {
  private static final Color INFO_COLOR = JBColor.namedColor("ToolTip.infoForeground", JBUI.CurrentTheme.ContextHelp.FOREGROUND);
  private static final Color LINK_COLOR = JBColor.namedColor("ToolTip.linkForeground", JBUI.CurrentTheme.Link.Foreground.ENABLED);

  private static final JBValue MAX_WIDTH = new JBValue.UIInteger("HelpTooltip.maxWidth", 250);
  private static final JBValue X_OFFSET = new JBValue.UIInteger("HelpTooltip.xOffset", 0);
  private static final JBValue Y_OFFSET = new JBValue.UIInteger("HelpTooltip.yOffset", 0);
  private static final JBValue HEADER_FONT_SIZE_DELTA = new JBValue.UIInteger("HelpTooltip.fontSizeDelta", 0);
  private static final JBValue DESCRIPTION_FONT_SIZE_DELTA = new JBValue.UIInteger("HelpTooltip.descriptionSizeDelta", 0);
  private static final JBValue CURSOR_OFFSET = new JBValue.UIInteger("HelpTooltip.mouseCursorOffset", 20);

  private static final String PARAGRAPH_SPLITTER = "<p/?>";
  private static final String TOOLTIP_PROPERTY = "JComponent.syncChangesHelpTooltip";
  private static final String TOOLTIP_DISABLED_PROPERTY = "JComponent.helpTooltipDisabled";

  private static final int LINKS_GAP = JBUI.scale(10);
  private final ArrayList<ActionLink> links = new ArrayList<>();
  private final ArrayList<@Nullable JBFontScaler> linkOriginalFontScalers = new ArrayList<>();
  private final Alarm popupAlarm = new Alarm();
  protected MouseAdapter myMouseListener;
  private @Nullable Supplier<@NotNull @TooltipTitle String> title;
  private @NlsSafe String shortcut;
  private @Tooltip String description;
  private NonOpaquePanel linksPanel;
  private boolean neverHide;
  private @NotNull Alignment alignment = Alignment.CURSOR;
  private BooleanSupplier masterPopupOpenCondition;
  private JBPopup myPopup;
  private boolean isOverPopup;
  private boolean isMultiline;
  private int myInitialDelay = -1;
  private int myHideDelay = -1;
  private String myToolTipText;
  private boolean initialShowScheduled;

  /**
   * Sets tooltip title.
   * If it's longer than two lines (fitting in 250 pixels each),
   * then the text is automatically stripped to the word boundary and dots are added to the end.
   *
   * @param title text for title.
   * @return {@code this}
   */
  public SyncChangesHelpTooltip setTitle(@Nullable @TooltipTitle String title) {
    this.title = title != null ? () -> title : null;
    return this;
  }

  public SyncChangesHelpTooltip setTitle(@Nullable Supplier<@NotNull @TooltipTitle String> title) {
    this.title = title;
    return this;
  }

  /**
   * Sets text for the shortcut placeholder.
   *
   * @param shortcut text for shortcut.
   * @return {@code this}
   */
  public SyncChangesHelpTooltip setShortcut(@Nullable @NlsSafe String shortcut) {
    this.shortcut = shortcut;
    return this;
  }

  public SyncChangesHelpTooltip setShortcut(@Nullable Shortcut shortcut) {
    this.shortcut = shortcut == null ? null : KeymapUtil.getShortcutText(shortcut);
    return this;
  }

  /**
   * Set HelpTooltip initial delay. Tooltip is show after component's mouse enter plus initial delay.
   *
   * @param delay - non negative value for initial delay
   * @return {@code this}
   * @throws IllegalArgumentException if delay is less than zero
   */
  public SyncChangesHelpTooltip setInitialDelay(int delay) {
    if (delay < 0) {
      throw new IllegalArgumentException("Negative delay is not allowed");
    }

    myInitialDelay = delay;
    return this;
  }

  /**
   * Set HelpTooltip hide delay. Tooltip is hidden after component's mouse exit plus hide delay.
   *
   * @param delay - non negative value for hide delay
   * @return {@code this}
   * @throws IllegalArgumentException if delay is less than zero
   */
  public SyncChangesHelpTooltip setHideDelay(int delay) {
    if (delay < 0) {
      throw new IllegalArgumentException("Negative delay is not allowed");
    }

    myHideDelay = delay;
    return this;
  }

  /**
   * Sets description text.
   *
   * @param description text for description.
   * @return {@code this}
   */
  public SyncChangesHelpTooltip setDescription(@Nullable @Tooltip String description) {
    this.description = description;
    return this;
  }

  /**
   * Enables link in the tooltip below description and sets action for it.
   *
   * @param linkText   text to show in the link.
   * @param linkAction action to execute when link is clicked.
   * @return {@code this}
   */
  public SyncChangesHelpTooltip addLink(@NlsContexts.LinkLabel String linkText, Runnable linkAction) {
    return addLink(linkText, linkAction, false);
  }

  /**
   * Enables link in the tooltip below description and sets action for it.
   *
   * @param linkText   text to show in the link.
   * @param linkAction action to execute when link is clicked.
   * @param external   whether the link is "external" or not
   * @return {@code this}
   */
  public SyncChangesHelpTooltip addLink(@NlsContexts.LinkLabel String linkText, Runnable linkAction, boolean external) {
    ActionLink link = new ActionLink(linkText, e -> {
      hidePopup(true);
      linkAction.run();
    });
    if (external) {
      link.setExternalLinkIcon();
    }
    links.add(link);
    linkOriginalFontScalers.add(new JBFontScaler(link.getFont()));
    return this;
  }

  /**
   * Enables link in the tooltip below description and sets BrowserUtil.browse action for it.
   * It's then painted with a small arrow button.
   *
   * @param linkLabel text to show in the link.
   * @param url       URL to browse.
   * @return {@code this}
   */
  public SyncChangesHelpTooltip setBrowserLink(@NlsContexts.LinkLabel String linkLabel, URL url) {
    ActionLink link = new BrowserLink(linkLabel, url.toExternalForm());
    link.setHorizontalTextPosition(SwingConstants.LEFT);
    links.add(link);
    return this;
  }

  public SyncChangesHelpTooltip clearLinks() {
    links.clear();
    linkOriginalFontScalers.clear();
    return this;
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) return true;
    if (that == null || getClass() != that.getClass()) return false;
    SyncChangesHelpTooltip tooltip = (SyncChangesHelpTooltip)that;
    return neverHide == tooltip.neverHide &&
           (title == null ? tooltip.title == null
                          : tooltip.title != null && Objects.equals(title.get(), tooltip.title.get())) &&
           Objects.equals(shortcut, tooltip.shortcut) &&
           Objects.equals(description, tooltip.description) &&
           Objects.equals(links, tooltip.links) &&
           alignment == tooltip.alignment &&
           Objects.equals(masterPopupOpenCondition, tooltip.masterPopupOpenCondition);
  }

  /**
   * Toggles whether to hide tooltip automatically on timeout. For default behavior just don't call this method.
   *
   * @param neverHide {@code true} don't hide, {@code false} otherwise.
   * @return {@code this}
   */
  public SyncChangesHelpTooltip setNeverHideOnTimeout(boolean neverHide) {
    this.neverHide = neverHide;
    return this;
  }

  /**
   * Sets location of the tooltip relatively to the owner component.
   *
   * @param alignment is relative location
   * @return {@code this}
   */
  public SyncChangesHelpTooltip setLocation(@NotNull Alignment alignment) {
    this.alignment = alignment;
    return this;
  }

  /**
   * Installs the tooltip after the configuration has been completed on the specified owner component.
   *
   * @param component is the owner component for the tooltip.
   */
  public void installOn(@NotNull JComponent component) {
    SyncChangesHelpTooltip installed = (SyncChangesHelpTooltip)component.getClientProperty(TOOLTIP_PROPERTY);

    if (installed == null) {
      installImpl(component);
    }
    else if (!equals(installed)) {
      installed.hideAndDispose(component);
      installImpl(component);
    }
  }

  private void installImpl(@NotNull JComponent component) {
    neverHide = neverHide || UIUtil.isHelpButton(component);

    createMouseListeners();

    component.putClientProperty(TOOLTIP_PROPERTY, this);
    installMouseListeners(component);
  }

  protected final void createMouseListeners() {
    myMouseListener = new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        if (myPopup != null && !myPopup.isDisposed()) {
          myPopup.cancel();
        }
        initialShowScheduled = true;
        int delay = myInitialDelay;
        if (delay == -1) {
          delay = Registry.intValue("ide.tooltip.initialReshowDelay", 500);
        }
        scheduleShow(e, delay);
      }

      @Override
      public void mouseExited(MouseEvent e) {
        int delay = myHideDelay;
        if (delay == -1) {
          delay = Registry.intValue("ide.tooltip.initialDelay.highlighter", 150);
        }
        scheduleHide(links.isEmpty(), delay);
      }

      @Override
      public void mouseMoved(MouseEvent e) {
        if (!initialShowScheduled) {
          scheduleShow(e, Registry.intValue("ide.tooltip.reshowDelay"));
        }
      }
    };
  }

  private @NotNull MouseListener createIsOverTipMouseListener() {
    return new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        isOverPopup = true;
      }

      @Override
      public void mouseExited(MouseEvent e) {
        if (links.isEmpty() || !linksPanel.getBounds().contains(e.getPoint())) {
          isOverPopup = false;
          hidePopup(false);
        }
      }
    };
  }

  @ApiStatus.Internal
  public final @NotNull JPanel createTipPanel() {
    JPanel tipPanel = new JPanel();
    tipPanel.setLayout(new VerticalLayout(JBUI.getInt("HelpTooltip.verticalGap", 4)));
    tipPanel.setBackground(UIUtil.getToolTipBackground());

    String currentTitle = title != null ? title.get() : null;
    boolean hasTitle = Strings.isNotEmpty(currentTitle);
    boolean hasDescription = Strings.isNotEmpty(description);

    if (hasTitle) {
      tipPanel.add(new Header(hasDescription), VerticalLayout.TOP);
    }

    if (hasDescription) {
      @Nls String[] pa = description.split(PARAGRAPH_SPLITTER);
      isMultiline = pa.length > 1;
      for (String p : pa) {
        if (!p.isEmpty()) {
          tipPanel.add(new Paragraph(p, hasTitle), VerticalLayout.TOP);
        }
      }
    }

    if (!hasTitle && Strings.isNotEmpty(shortcut)) {
      JLabel shortcutLabel = new JLabel(shortcut);
      shortcutLabel.setFont(deriveDescriptionFont(shortcutLabel.getFont(), false));
      shortcutLabel.setForeground(JBUI.CurrentTheme.Tooltip.shortcutForeground());

      tipPanel.add(shortcutLabel, VerticalLayout.TOP);
    }

    if (!links.isEmpty()) {
      linksPanel = new NonOpaquePanel(new HorizontalLayout(LINKS_GAP));
      for (int i = 0; i < links.size(); i++) {
        JBFontScaler fontScaler = linkOriginalFontScalers.get(i);
        if (fontScaler == null) continue;
        links.get(i).setForeground(LINK_COLOR);
        links.get(i).setFont(deriveDescriptionFont(fontScaler.scaledFont(), hasTitle));
        linksPanel.add(links.get(i));
      }
      tipPanel.add(linksPanel, VerticalLayout.TOP);
    }

    isMultiline = isMultiline || Strings.isNotEmpty(description) && (Strings.isNotEmpty(currentTitle) || !links.isEmpty());
    tipPanel.setBorder(textBorder(isMultiline));

    return tipPanel;
  }

  private void installMouseListeners(@NotNull JComponent owner) {
    owner.addMouseListener(myMouseListener);
    owner.addMouseMotionListener(myMouseListener);
  }

  private void uninstallMouseListeners(@NotNull JComponent owner) {
    owner.removeMouseListener(myMouseListener);
    owner.removeMouseMotionListener(myMouseListener);
  }

  private void hideAndDispose(@NotNull JComponent owner) {
    hidePopup(true);
    uninstallMouseListeners(owner);
    masterPopupOpenCondition = null;
    owner.putClientProperty(TOOLTIP_PROPERTY, null);
  }

  private void scheduleShow(MouseEvent e, int delay) {
    popupAlarm.cancelAllRequests();
    if (isTooltipDisabled(e.getComponent())) return;
    if (ScreenReader.isActive()) return; // Disable HelpTooltip in screen reader mode.

    popupAlarm.addRequest((ContextAwareRunnable)() -> {
      initialShowScheduled = false;
      if (masterPopupOpenCondition != null && !masterPopupOpenCondition.getAsBoolean()) {
        return;
      }

      Component owner = e.getComponent();
      String text = owner instanceof JComponent ? ((JComponent)owner).getToolTipText(e) : null;
      if (myPopup != null && !myPopup.isDisposed()) {
        if (Strings.isEmpty(text) && Strings.isEmpty(myToolTipText)) {
          return; // do nothing if a tooltip become empty
        }
        if (Objects.equals(text, myToolTipText)) {
          return; // do nothing if a tooltip is not changed
        }
        myPopup.cancel(); // cancel previous popup before showing a new one
      }

      myToolTipText = text;
      JComponent tipPanel = createTipPanel();
      tipPanel.addMouseListener(createIsOverTipMouseListener());
      ComponentPopupBuilder popupBuilder = initPopupBuilder(tipPanel);
      myPopup = popupBuilder.createPopup();
      myPopup.show(new RelativePoint(owner, alignment.getPointFor(owner, tipPanel.getPreferredSize(), e.getPoint())));
      if (!neverHide) {
        int dismissDelay = Registry.intValue(isMultiline ? "ide.helptooltip.full.dismissDelay" : "ide.helptooltip.regular.dismissDelay");
        scheduleHide(true, dismissDelay);
      }
    }, delay);
  }

  private void scheduleHide(boolean force, int delay) {
    popupAlarm.cancelAllRequests();
    popupAlarm.addRequest((ContextAwareRunnable)() -> hidePopup(force), delay);
  }

  protected void hidePopup(boolean force) {
    initialShowScheduled = false;
    popupAlarm.cancelAllRequests();

    if (myPopup != null && (!isOverPopup || force)) {
      if (myPopup.isVisible()) myPopup.cancel();
      myPopup = null;
      myToolTipText = null;
    }
  }

  boolean fromSameWindowAs(@NotNull Component component) {
    if (myPopup != null && !myPopup.isDisposed()) {
      Window popupWindow = SwingUtilities.getWindowAncestor(myPopup.getContent());
      return component == popupWindow || SwingUtilities.getWindowAncestor(component) == popupWindow;
    }
    return false;
  }

  @ApiStatus.Internal
  public static ComponentPopupBuilder initPopupBuilder(@NotNull JComponent tipPanel) {
    return JBPopupFactory.getInstance().
      createComponentPopupBuilder(tipPanel, null).
      setShowBorder(UIManager.getBoolean("ToolTip.paintBorder")).
      setBorderColor(JBUI.CurrentTheme.Tooltip.borderColor()).setShowShadow(true).
      addUserData(PopupCornerType.RoundedTooltip);
  }

  public static @Nullable SyncChangesHelpTooltip getTooltipFor(@NotNull JComponent owner) {
    return (SyncChangesHelpTooltip)owner.getClientProperty(TOOLTIP_PROPERTY);
  }

  /**
   * Hides and disposes the tooltip possibly installed on the mentioned component. Disposing means
   * unregistering all {@code HelpTooltip} specific listeners installed on the component.
   * If there is no tooltip installed on the component nothing happens.
   *
   * @param owner a possible {@code HelpTooltip} owner.
   */
  public static void dispose(@NotNull Component owner) {
    if (owner instanceof JComponent component) {
      SyncChangesHelpTooltip instance = (SyncChangesHelpTooltip)component.getClientProperty(TOOLTIP_PROPERTY);
      if (instance != null) {
        instance.hideAndDispose(component);
      }
    }
  }

  /**
   * Hides the tooltip possibly installed on the mentioned component without disposing.
   * Listeners are not removed.
   * If there is no tooltip installed on the component nothing happens.
   *
   * @param owner a possible {@code HelpTooltip} owner.
   */
  public static void hide(@NotNull Component owner) {
    if (owner instanceof JComponent) {
      SyncChangesHelpTooltip instance = (SyncChangesHelpTooltip)((JComponent)owner).getClientProperty(TOOLTIP_PROPERTY);
      if (instance != null) {
        instance.hidePopup(true);
      }
    }
  }

  /**
   * Sets master popup for the current {@code HelpTooltip}. Master popup takes over the help tooltip,
   * so when the master popup is about to be shown, help tooltip hides.
   *
   * @param owner  possible owner
   * @param master master popup
   */
  public static void setMasterPopup(@NotNull Component owner, @Nullable JBPopup master) {
    if (owner instanceof JComponent) {
      SyncChangesHelpTooltip tooltip = (SyncChangesHelpTooltip)((JComponent)owner).getClientProperty(TOOLTIP_PROPERTY);
      if (tooltip != null && tooltip.myPopup != master) {
        WeakReference<JBPopup> popupRef = new WeakReference<>(master);
        tooltip.masterPopupOpenCondition = () -> {
          JBPopup popup = SoftReference.dereference(popupRef);
          return popup == null || !popup.isVisible();
        };
      }
    }
  }

  /**
   * Sets master popup open condition supplier for the current {@code HelpTooltip}.
   * This method is more general than {@link com.intellij.ide.HelpTooltip#setMasterPopup(Component, JBPopup)} so that
   * it's possible to create master popup condition for any types of popups such as {@code JPopupMenu}
   *
   * @param owner     possible owner
   * @param condition a {@code BooleanSupplier} for open condition
   */
  public static void setMasterPopupOpenCondition(@NotNull Component owner, @Nullable BooleanSupplier condition) {
    if (owner instanceof JComponent) {
      SyncChangesHelpTooltip instance = (SyncChangesHelpTooltip)((JComponent)owner).getClientProperty(TOOLTIP_PROPERTY);
      if (instance != null) {
        instance.masterPopupOpenCondition = condition;
      }
    }
  }

  public static void disableTooltip(Component source) {
    if (source instanceof JComponent component) {
      component.putClientProperty(TOOLTIP_DISABLED_PROPERTY, Boolean.TRUE);
    }
  }

  public static void enableTooltip(Component source) {
    if (source instanceof JComponent component) {
      component.putClientProperty(TOOLTIP_DISABLED_PROPERTY, null);
    }
  }

  private static boolean isTooltipDisabled(Component component) {
    if (component instanceof JComponent jComponent) {
      Boolean disabled = (Boolean)jComponent.getClientProperty(TOOLTIP_DISABLED_PROPERTY);
      return disabled == Boolean.TRUE;
    }
    else {
      return false;
    }
  }

  private static Border textBorder(boolean multiline) {
    Insets i = multiline ? JBUI.CurrentTheme.HelpTooltip.defaultTextBorderInsets() : JBUI.CurrentTheme.HelpTooltip.smallTextBorderInsets();
    return new JBEmptyBorder(i);
  }

  private static Font deriveHeaderFont(Font font) {
    return font.deriveFont((float)font.getSize() + HEADER_FONT_SIZE_DELTA.get());
  }

  private static Font deriveDescriptionFont(Font font, boolean hasTitle) {
    return hasTitle ?
           font.deriveFont((float)font.getSize() + DESCRIPTION_FONT_SIZE_DELTA.get()) :
           deriveHeaderFont(font);
  }

  @Contract(pure = true)
  public static @NotNull String getShortcutAsHtml(@Nullable String shortcut) {
    return Strings.isEmpty(shortcut)
           ? ""
           : String.format("&nbsp;&nbsp;<font color=\"%s\">%s</font>",
                           ColorUtil.toHtmlColor(JBUI.CurrentTheme.Tooltip.shortcutForeground()),
                           shortcut);
  }

  /**
   * Location of the HelpTooltip relatively to the owner component.
   */
  public enum Alignment {
    RIGHT {
      @Override
      public Point getPointFor(Component owner, Dimension popupSize, Point mouseLocation) {
        Dimension size = owner.getSize();
        return new Point(size.width + JBUIScale.scale(5) - X_OFFSET.get(), JBUIScale.scale(1) + Y_OFFSET.get());
      }
    },

    LEFT {
      @Override
      public Point getPointFor(Component owner, Dimension popupSize, Point mouseLocation) {
        return new Point(-popupSize.width - JBUIScale.scale(5) + X_OFFSET.get(), JBUIScale.scale(1) + Y_OFFSET.get());
      }
    },

    BOTTOM {
      @Override
      public Point getPointFor(Component owner, Dimension popupSize, Point mouseLocation) {
        Dimension size = owner.getSize();
        return new Point(JBUIScale.scale(1) + X_OFFSET.get(), JBUIScale.scale(5) + size.height - Y_OFFSET.get());
      }
    },

    HELP_BUTTON {
      @Override
      public Point getPointFor(Component owner, Dimension popupSize, Point mouseLocation) {
        Insets i = ((JComponent)owner).getInsets();
        return new Point(X_OFFSET.get() - JBUIScale.scale(40), i.top + Y_OFFSET.get() - JBUIScale.scale(6) - popupSize.height);
      }
    },

    CURSOR {
      @Override
      public Point getPointFor(Component owner, Dimension popupSize, Point mouseLocation) {
        Point location = mouseLocation.getLocation();
        location.y += CURSOR_OFFSET.get();

        SwingUtilities.convertPointToScreen(location, owner);
        Rectangle r = new Rectangle(location, popupSize);
        ScreenUtil.fitToScreen(r);
        location = r.getLocation();
        SwingUtilities.convertPointFromScreen(location, owner);
        r.setLocation(location);

        if (r.contains(mouseLocation)) {
          location.y = mouseLocation.y - r.height - JBUI.scale(5);
        }

        return location;
      }
    },

    /**
     * The position of the tooltip at which the tooltip is shown directly on the cursor
     * The common problem with {@link Alignment#CURSOR} is following:
     * MouseListener leaves the current node and enters the neighboring node, which creates difficulties.
     * Therefore, the following hack was used:
     * Slightly change the position of the tooltip display so that the tooltip is displayed at the exact cursor position
     */
    EXACT_CURSOR {
      @Override
      public Point getPointFor(Component owner, Dimension popupSize, Point mouseLocation) {
        Point location = mouseLocation.getLocation();

        SwingUtilities.convertPointToScreen(location, owner);
        Rectangle r = new Rectangle(location, popupSize);
        ScreenUtil.fitToScreen(r);
        location = r.getLocation();
        SwingUtilities.convertPointFromScreen(location, owner);
        r.setLocation(location);

        return location;
      }
    };

    public abstract Point getPointFor(Component owner, Dimension popupSize, Point mouseLocation);
  }

  private static class BoundWidthLabel extends JLabel {
    void setSizeForWidth(float width) {
      if (width > MAX_WIDTH.get()) {
        View v = (View)getClientProperty(BasicHTML.propertyKey);
        if (v != null) {
          width = 0.0f;
          for (View row : getRows(v)) {
            float rWidth = row.getPreferredSpan(View.X_AXIS);
            if (width < rWidth) {
              width = rWidth;
            }
          }

          v.setSize(width, v.getPreferredSpan(View.Y_AXIS));
        }
      }
    }

    private static Collection<View> getRows(@NotNull View root) {
      Collection<View> rows = new ArrayList<>();
      visit(root, rows);
      return rows;
    }

    private static void visit(@NotNull View v, Collection<? super View> result) {
      String cname = v.getClass().getCanonicalName();
      if (cname != null && cname.contains("ParagraphView.Row")) {
        result.add(v);
      }

      for (int i = 0; i < v.getViewCount(); i++) {
        visit(v.getView(i), result);
      }
    }
  }

  private final class Header extends BoundWidthLabel {
    private Header(boolean obeyWidth) {
      setFont(deriveHeaderFont(getFont()));
      setForeground(UIUtil.getToolTipForeground());

      String currentTitle = Objects.requireNonNullElse(title != null ? title.get() : null, "");
      if (obeyWidth || currentTitle.length() > MAX_WIDTH.get()) {
        View v = BasicHTML.createHTMLView(this, String.format("<html>%s%s</html>", currentTitle, getShortcutAsHTML()));
        float width = v.getPreferredSpan(View.X_AXIS);
        isMultiline = isMultiline || width > MAX_WIDTH.get();
        HtmlChunk.Element div = width > MAX_WIDTH.get() ? HtmlChunk.div().attr("width", MAX_WIDTH.get()) : HtmlChunk.div();
        setText(div.children(HtmlChunk.raw(currentTitle), HtmlChunk.raw(getShortcutAsHTML()))
                  .wrapWith(html())
                  .toString());
        setSizeForWidth(width);
      }
      else {
        setText(BasicHTML.isHTMLString(currentTitle) ?
                currentTitle :
                HtmlChunk.div().addRaw(currentTitle).addRaw(getShortcutAsHTML()).wrapWith(html()).toString());
      }
    }

    private @NlsSafe String getShortcutAsHTML() {
      return getShortcutAsHtml(shortcut);
    }
  }

  private final class Paragraph extends BoundWidthLabel {
    private Paragraph(@Tooltip String text, boolean hasTitle) {
      setForeground(hasTitle ? INFO_COLOR : UIUtil.getToolTipForeground());
      setFont(deriveDescriptionFont(getFont(), hasTitle));

      View v = BasicHTML.createHTMLView(this, HtmlChunk.raw(text).wrapWith(html()).toString());
      float width = v.getPreferredSpan(View.X_AXIS);
      isMultiline = isMultiline || width > MAX_WIDTH.get();
      HtmlChunk.Element div = width > MAX_WIDTH.get() ? HtmlChunk.div().attr("width", MAX_WIDTH.get()) : HtmlChunk.div();
      setText(div.addRaw(text).wrapWith(html()).toString());

      setSizeForWidth(width);
    }
  }
}
