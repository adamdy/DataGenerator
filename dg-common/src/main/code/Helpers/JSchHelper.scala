/*
 * Copyright 2014 DataGenerator Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package Helpers

import java.io.{BufferedReader, FileWriter, InputStreamReader}
import java.text.SimpleDateFormat
import java.util.Date
import Helpers.StringHelper.StringImplicits
import com.jcraft.jsch._

import scala.beans.BooleanBeanProperty

/**
 * Helper methods for SFTP and SSH exec using the Java JSch library
 */
object JSchHelper {
  /**
   * Whether or not to log remote commands.
   */
  @BooleanBeanProperty
  var logRemoteCommands = true

  implicit class ChannelImplicits(private val channel: Channel) {
    private final val SLEEP_ON_RETRY_MS = 50

    /**
     * Get allowable JSCH channel types
     * @return
     */
    def channelType: String = {
      channel match {
        case _: ChannelExec => "exec"
        case _: ChannelSftp => "sftp"
        case _: ChannelShell => "shell"
        //case _: ChannelX11 => "x11"
        //case _: ChannelAgentForwarding => "auth-agent@openssh.com"
        case _: ChannelDirectTCPIP => "direct-tcpip"
        case _: ChannelForwardedTCPIP => "forwarded-tcpip"
        case _: ChannelSubsystem => "subsystem"
      }
    }

    /**
     * Connect to the channel using optional timeout and number of tries.
     * @param timeout -1 for no timeout, else milliseconds before connect attempt fails.
     * @param tries Number of tries before failing.
     */
    def connectWithRetry(timeout: Int = -1, tries: Short = 10): Unit = {
      if (timeout < 0) {
        RetryHelper.retry(
          tries, Seq(classOf[JSchException]))(
            channel.connect())(
            try { Thread.sleep(SLEEP_ON_RETRY_MS); channel.getSession.openChannel(channelType); Thread.sleep(SLEEP_ON_RETRY_MS)
            } catch{case _:JSchException => {}})
      } else {
        RetryHelper.retry(
          tries, Seq(classOf[JSchException]))(
            channel.connect(timeout))(
            try { Thread.sleep(SLEEP_ON_RETRY_MS); channel.getSession.openChannel(channelType); Thread.sleep(SLEEP_ON_RETRY_MS)
            } catch{case _:JSchException => {}})
      }
    }
  }

  /**
   * Implicit methods on an Exec channel.
   * @param channelExec
   */
  implicit class ExecImplicits(private val channelExec: ChannelExec) {
    /**
     * Define the command (including any parameters) to execute remotely over SSH.
     * @param command
     */
    def setCommandToExec(command: String): Unit = {
      if (logRemoteCommands) {
        println(s"${new SimpleDateFormat("yyyy_MM_dd HH-mm-ss") // scalastyle:ignore
          .format(new Date())}: Executing remote command on ${channelExec.getSession.getHost}: $command")
      }
      channelExec.setCommand(command)
    }

    /**
     * Run a command over SSH exec channel and save the output to a local text file.
     * @param command Command to run remotely
     * @param localFilePath Local path to save stdout of remote command
     * @return Exit code
     */
    def runCommandAndSaveOutputLocally(command: String, localFilePath: String): Int = {
      val inputStream = new BufferedReader(new InputStreamReader(channelExec.getInputStream))
      var writerMaybe: Option[FileWriter] = None
      channelExec.setCommandToExec(command)
      channelExec.connectWithRetry(3000)
      try {
        while (!channelExec.isClosed || inputStream.ready) {
          if (inputStream.ready) {
            if (writerMaybe.isEmpty) {
              writerMaybe = Some(new FileWriter(localFilePath))
            }
            writerMaybe.get.write(s"${inputStream.readLine()}\r\n")
          }
        }
        channelExec.getExitStatus
      } finally {
        if (writerMaybe.nonEmpty) {
          writerMaybe.get.close()
        }
        inputStream.close()
        channelExec.disconnect()
      }
    }
  }

  /**
   * Implicit methods on an SFTP channel
   * @param sftpChannel
   */
  implicit class SftpImplicits(private val sftpChannel: ChannelSftp) {
    /**
     * Download a file over SFTP to local, with some retries in case of failure.
     * @param src
     * @param dest
     * @param triesBeforeFailure
     */
    def download(src: String, dest: String, triesBeforeFailure: Short = 3): Unit = {
      if (logRemoteCommands) {
        println(s"${new SimpleDateFormat("yyyy_MM_dd HH-mm-ss") // scalastyle:ignore
          .format(new Date())}: Downloading from ${sftpChannel.getSession.getHost}: `$src` to `$dest`")
      }
      RetryHelper.retry[Unit](3, Seq(classOf[SftpException]))(sftpChannel.get(src, dest))()
    }

    /**
     * Upload a file over SFTP from local, with some retries in case of failure.
     * @param src
     * @param dest
     * @param mode ChannelSftp mode, e.g., whether or not to overwrite
     * @param triesBeforeFailure
     */
    def upload(src: String, dest: String, mode: Int = ChannelSftp.OVERWRITE, triesBeforeFailure: Short = 3): Unit = {
      if (logRemoteCommands) {
        println(s"${new SimpleDateFormat("yyyy_MM_dd HH-mm-ss") // scalastyle:ignore
          .format(new Date())}: Uploading to ${sftpChannel.getSession.getHost}: `$src` to `$dest`")
      }
      RetryHelper.retry[Unit](3, Seq(classOf[SftpException]))(sftpChannel.put(src, dest, mode))()
    }

    /**
     * Create a remote directory if it doesn't alraedy exist, and if it does, empty it.
     * @param dirPath
     * @param triesBeforeFailure
     */
    def ensureEmptyDirectoryExists(dirPath: String, triesBeforeFailure: Short = 3): Unit = {
      sftpChannel.mkdirRecursivelyIfNotExists(dirPath)
      sftpChannel.cd(dirPath)
      if (logRemoteCommands) {
        println(s"${new SimpleDateFormat("yyyy_MM_dd HH-mm-ss").format(new Date())}: Deleting * from $dirPath") // scalastyle:ignore
      }
      RetryHelper.retry[Unit](3, Seq(classOf[SftpException]))(sftpChannel.rm("*"))()
    }

    /**
     * Create a remote directory if it doesn't already exist.
     * @param recursiveDirToCreate
     */
    def mkdirRecursivelyIfNotExists(recursiveDirToCreate: String): Unit = {
      assert(!sftpChannel.isClosed, "SFTP channel must be open!")

      var nextDirToAdd = "/"
      val dirParts = recursiveDirToCreate.splitOnChar('/')

      dirParts.foreach(dirPart => {
        nextDirToAdd += s"${dirPart}/"

        // Instead of check-if-exists (using ls or stat) and then create if not exists, we always try to create and
        // ignore the error that will result if it already exists (SSH_FX_FAILURE).
        // Just doing mkdir without checking is less network IO.
        try {
          sftpChannel.mkdir(nextDirToAdd)
        } catch {
            case e: SftpException => if (e.id != ChannelSftp.SSH_FX_FAILURE && nextDirToAdd == recursiveDirToCreate) throw e
        }
      })
    }
  }
}
