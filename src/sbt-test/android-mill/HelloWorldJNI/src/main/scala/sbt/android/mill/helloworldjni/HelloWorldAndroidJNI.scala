package sbt.android.mill.helloworldjni

object HelloWorldAndroidJNI {
  System.loadLibrary("HelloWorldAndroidJNI")

  /** 
   * Adds two integers, returning their sum
   */
  @native
  def add(v1: Int, v2: Int): Int;

  /**
   * Returns Hello World string
   */
  @native
  def hello(): String;
}