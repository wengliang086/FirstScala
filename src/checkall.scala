#!/bin/sh 
source /data/bin/common_env.sh
WORKSPACE=/data/bin/scala

LIB_DIR=$WORKSPACE/libs
LIB_JARS=`ls $LIB_DIR|grep .jar|awk '{print "'$LIB_DIR'/"$0}'|tr "\n" ":"`
echo $LIB_JARS

ips=`for (( m=0 m<${#ips[@]} m++  ))do echo ${ips[$m]}done |tr "\n" ","`
echo $ips
exec env ips="$ips" /data/bin/scala/scala-2.11.8/bin/scala -classpath  /data/bin/scala/conf:$LIB_JARS  "$0" "$@"
!#

import com.hoolai.fastaccess.common.hutil.tencentcloud.QCloudApi
import com.hoolai.fastaccess.common.hutil.tencentcloud.checker.{Checkers, QCheckList}
import com.hoolai.fastaccess.common.hutil.tencentcloud.vo.QCvm

import scala.collection.JavaConverters._

def checkAll(ip: String): Boolean = {
  var qCheckList: QCheckList = new QCheckList()
  qCheckList.add(Checkers.createOpenApi("access_open_api", ip, "/access_open_api/test/ok.hl"))
  qCheckList.add(Checkers.createOpenApi("charge_open_api", ip, "/charge_open_api/test/ok.hl"))
  qCheckList.add(Checkers.createOpenApi("message_service", ip, "/message_service/test/ok.hl"))
  qCheckList.add(Checkers.createOpenApi("access_web", ip + ":2050", "/access_web/test/ok.jspx"))
  qCheckList.add(Checkers.createProvider("access_provider", ip, 2040))
  qCheckList.add(Checkers.createProvider("user_provider", ip, 2030))
  qCheckList.add(Checkers.createProvider("log_provider", ip, 2110))
  qCheckList.add(Checkers.createProvider("query_provider", ip, 2120))
  qCheckList.check()
}

val secretid = "AKIDvjNVWOse0DF6m"
val secretkey = "UsGTVM2XpwpFYgfTCj6"
val region = "gz"
//val lbid    = "lb-04gybwnc" //内网
val lbid = "lb-jrlxv70m" //正式环境


val api: QCloudApi = new QCloudApi(secretid, secretkey, region)

def getCvmList: java.util.List[QCvm] = {
  var ips: String = sys.env("ips")
  var ipsArray: Array[String] = ips.split(",")
  var lanIps = ipsArray.map(s => s.split(" ")(0))
  var cvmlist = api.getCVM(lanIps.toList.asJava)
  cvmlist.asScala.foreach(cvm => ipsArray.foreach(s => if (s.split(" ")(0) == cvm.getLanIp) {
    cvm.setWeight(Integer.valueOf(s.split(" ")(2)))
    cvm.setGroup(Integer.valueOf(s.split(" ")(1)))
  }))
  cvmlist
}

getCvmList.asScala.foreach(cvm => {
  var result = checkAll(cvm.getLanIp)
  println(cvm.getInstanceName + ",部署" + result)
})

