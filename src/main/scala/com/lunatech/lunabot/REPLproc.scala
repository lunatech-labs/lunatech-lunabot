package com.lunatech.lunabot

import scala.sys.process._
import scala.io._
import java.io._

/**
 * scala REPL process
 */

object REPLproc {
  var respOut = ""
  var respErr = ""

 /**
  * Start a scala REPL process, write Scala code string to STDIN of it, and read from STDOUT and STDERR.
  * @param code String, Scala code to be executed.
  * @return String, scala REPL response, STDERR after STDOUT.
  * @example val responseLinesOfString = REPLproc.run(scalaCodeString)
  * @todo Remove the 2,3 lines of welcome message, and the last line of auto-generated ":quit".
  * @todo TODO: seperate STDOUT and STDERR to allow different coloring.
  */

  def run (code: String): String = {
    val cmd = Seq("scala") // Uses Seq here to allow possible command options later
    val process = Process (cmd)
    val io = new ProcessIO (
      in  => {new PrintStream(in).println(code); in.close},
      out => {respOut = Source.fromInputStream(out).getLines.mkString("\n"); out.close}, // TODO: Remove line 2,3 of welcome message, and the last line of auto-generated ":quit".
      err => {respErr = Source.fromInputStream(err).getLines.mkString("\n"); err.close})
    process.run(io).exitValue // Wait until the process exit, otherwise respOut and respErr won't get output yet.
    respOut + respErr // TODO: seperate STDOUT and STDERR to allow different coloring.
  }
}
