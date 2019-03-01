
import com.hoolai.fastaccess.common.hutil.tencentcloud.QCloudApi
import com.hoolai.fastaccess.common.hutil.tencentcloud.vo.QCvm

import scala.collection.JavaConverters._


val secretid = "AKIDvjNVWOse0DF6m"
val secretkey = "UsGTVM2XpwpFYgfTCj6"
val region = "gz"
val lbid = "lb-04gybwnc"


def getCvmList: java.util.List[QCvm] = {

  var ips: String = sys.env("ips")
  var ipsArray: Array[String] = ips.split(",")
  var lanIps = ipsArray.map(s => s.split(" ")(0))

  val api: QCloudApi = new QCloudApi(secretid, secretkey, region)

  var cvmlist = api.getCVM(lanIps.toList.asJava)

  cvmlist.asScala.foreach(cvm => ipsArray.foreach(s => if (s.split(" ")(0) == cvm.getLanIp) {
    cvm.setGroup(Integer.valueOf(s.split(" ")(1)))
  }))

  cvmlist
}


getCvmList.asScala.foreach(cvm => println(cvm.getLanIp))













