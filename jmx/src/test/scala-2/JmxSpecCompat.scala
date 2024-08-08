import javax.management.{ MBeanNotificationInfo, ObjectInstance }

import org.specs2.matcher.MatchResult

trait JmxSpecCompat { self: JmxSpec =>

  @inline protected def verifyBeanInstance(
      instance: => ObjectInstance,
      beanType: String,
      attrs: List[AttrDef],
      notifInfo: Array[MBeanNotificationInfo]
    ): MatchResult[Option[ObjectInstance]] =
    self.checkBeanInstance(instance, beanType, attrs, notifInfo)

}
