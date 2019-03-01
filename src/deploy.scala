#!/bin/sh 
source /data/bin/common_env.sh
WORKSPACE=/data/bin/scala

LIB_DIR=$WORKSPACE/libs
LIB_JARS=`ls $LIB_DIR|grep .jar|awk '{print "'$LIB_DIR'/"$0}'|tr "\n" ":"`
echo $LIB_JARS

ips=`for (( m=0; m<${#ips[@]}; m++  ));do echo ${ips[$m]};done |tr "\n" ","`
echo $ips
exec env ips="$ips" /data/bin/scala/scala-2.11.8/bin/scala -classpath  /data/bin/scala/conf:$LIB_JARS  "$0" "$@"
!#

import com.hoolai.fastaccess.common.hutil.tencentcloud.checker.{Checkers, QCheckList}
import com.hoolai.fastaccess.common.hutil.tencentcloud.vo.{QCloudInstance, QCvm}
import com.hoolai.fastaccess.common.hutil.tencentcloud.{Deployment, QCloudApi, QCloudRollingUpdate}

import scala.collection.JavaConverters._
import scala.sys.process._

def createQCloudInstance(unInstanceId: String, weight: Integer, group: Integer): QCloudInstance = {
  val qCloudInstance: QCloudInstance = new QCloudInstance()
  qCloudInstance.setUnInstanceId(unInstanceId)
  qCloudInstance.setWeight(weight)
  qCloudInstance.setGroup(group)
  qCloudInstance
}

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
//val lbid      = "lb-jrlxv70m" //正式环境
val lbid = "lb-ibjvlk07" //正式环境

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

if (args.length == 0) {
  println("请输入要部署的应用名称")
  sys.exit(1)
}


val instances: List[QCloudInstance] = getCvmList.asScala.map(cvm => {
  val qCloudInstance: QCloudInstance = new QCloudInstance()
  qCloudInstance.setUnInstanceId(cvm.getInstanceId)
  qCloudInstance.setWeight(cvm.getWeight)
  qCloudInstance.setGroup(cvm.getGroup)
  println(cvm.getInstanceId + "," + cvm.getWeight + "," + cvm.getGroup)
  qCloudInstance
}).toList


val rollingUpdate = new QCloudRollingUpdate(api, lbid, instances.asJava)
val app: String = args(0)

rollingUpdate.rollingUpdate(new Deployment() {
  def deploy(q: QCloudInstance): java.lang.Boolean = {
    var server = q.getInstanceName
    println("===========部署" + server + "开始=============")
    if ("s1".equals(server) || "s2".equals(server)) {
      Process("/data/bin/scala/rsync_restart_remote.sh  " + q.getLanIp + " " + app).!
    } else {
      var result = Process("/data/bin/scala/rsync_restart_remote.sh  " + q.getLanIp + " " + app).!!
      try {
        Thread.sleep(5 * 1000)
      } catch {
        case ex: InterruptedException =>
          ex.printStackTrace()
          return false
      }
    }
    var result: Boolean = checkAll(q.getLanIp)
    println("===========部署" + server + "," + result + "=============")
    result
  }
})
