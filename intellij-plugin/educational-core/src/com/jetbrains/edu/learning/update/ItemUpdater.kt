package com.jetbrains.edu.learning.update

import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.submissions.isSignificantlyAfter

sealed interface ItemUpdater<T : StudyItem> {
  fun T.isOutdated(remoteItem: T): Boolean
}

interface HyperskillItemUpdater<T : StudyItem> : ItemUpdater<T> {
  override fun T.isOutdated(remoteItem: T): Boolean = remoteItem.updateDate.isSignificantlyAfter(updateDate)
}
