package com.lunatech.lunabot

import org.scalatest.FunSuite

class REPLprocTest extends FunSuite {

  // TODO All tests should be adjusted pending a better REPLproc implementation

  test("Simple empty expression ")  {

    val expression = ""
    val expected = ""
    assert(REPLproc.run(expression).get.contains(expected))
  }

  test("Simple add expression ")  {

    val expression = "1 + 1"
    val expected = "res0: Int = 2"

    assert(REPLproc.run(expression).get.contains(expected))

  }


  test("Simple division expression generating exception")  {

    val expression = "1 / 0"
    val expected = "java.lang.ArithmeticException: / by zero"

    assert(REPLproc.run(expression).get.contains(expected))

  }

  test("Simple add expression with quotes")  {

    val expression = "\"1 + 1 = \" + (1 + 1)"
    val expected = "res0: String = 1 + 1 = 2"

    assert(REPLproc.run(expression).get.contains(expected))

  }

  test("Simple add expression with import")  {

    val expression = "import scala.concurrent.duration.Duration; " +
      "Duration.Zero"
    val expected = "res0: scala.concurrent.duration.FiniteDuration = 0 days"

    assert(REPLproc.run(expression).get.contains(expected))

  }

  test("Simple for expression")  {

    val expression = "for(i <- 0 to 1) yield i "
    val expected = "res0: scala.collection.immutable.IndexedSeq[Int] = Vector(0, 1)"

    assert(REPLproc.run(expression).get.contains(expected))

  }

  test("IO expression writing to file") {

    //TODO: Probably in the future this test should fail as soon as we limit the features of the REPL (no IO...)
    import java.io._
    val outFileName = "/tmp/temp-test-file-name.tmp"
    val outFile = new File(outFileName)
    val expression = "import java.io._; " +
      "val file = new File(\""+ outFileName + "\"); " +
      "val bw = new BufferedWriter(new FileWriter(file)); " +
      "bw.write(\"test\"); " +
      "bw.close()"
    val expected = "file: java.io.File = /tmp/temp-test-file-name.tmp"

    assert(REPLproc.run(expression).get.contains(expected))
    assert(outFile.exists())
    outFile.delete()

  }

}

