package org.hyperskill.academy.learning.framework.impl.migration

import com.intellij.util.io.DataInputOutputUtil
import org.hyperskill.academy.learning.framework.impl.Change
import org.hyperskill.academy.learning.framework.impl.FrameworkStorageData
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

data class UserChanges1(val changes: List<Change>, val timestamp: Long) : FrameworkStorageData {

  @Throws(IOException::class)
  override fun write(out: DataOutput) {
    DataInputOutputUtil.writeINT(out, changes.size)
    changes.forEach { Change.writeChange(it, out) }
    DataInputOutputUtil.writeLONG(out, timestamp)
  }

  companion object {

    @Throws(IOException::class)
    fun read(input: DataInput): UserChanges1 {
      val size = DataInputOutputUtil.readINT(input)
      val changes = ArrayList<Change>(size)
      for (i in 0 until size) {
        changes += Change.readChange(input)
      }
      val timestamp = DataInputOutputUtil.readLONG(input)
      return UserChanges1(changes, timestamp)
    }
  }
}
