import javax.management.{ MBeanNotificationInfo, ObjectInstance }

trait JmxSpecCompat { self: JmxSpec =>

  inline protected def verifyBeanInstance(
      instance: => ObjectInstance,
      beanType: String,
      attrs: List[AttrDef],
      notifInfo: Array[MBeanNotificationInfo]
    ) = self.checkBeanInstance(instance, beanType, attrs, notifInfo)

}
