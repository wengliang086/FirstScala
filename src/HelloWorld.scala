object HelloWorld {
  def main(args: Array[String]): Unit = {
    println("Hello, world! ")

    //方法
    def m2(f: (Int, Int) => Int) = f(10, 4)

    //函数
    val f2 = (x: Int, y: Int) => x - y

    println(m2(f2))
    //匿名函数
    println(m2((x: Int, y: Int) => x + y))

    //    闭包
    var factor = 3
    val multiplier = (i: Int) => i * factor

    println(multiplier(1))
    println(multiplier(2))
  }
}
