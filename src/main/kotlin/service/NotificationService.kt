package service

interface OrderUpdateObserver {
    fun onOrderUpdated(orderId: Int, status: String)
}

interface OrderUpdateSubject {
    fun attach(observer: OrderUpdateObserver)
    fun detach(observer: OrderUpdateObserver)
    fun notifyUpdate(orderId: Int, status: String)
}

class UserNotificationService : OrderUpdateObserver {
    override fun onOrderUpdated(orderId: Int, status: String) {
        if (status == "COOKED") {
            println("Notification: Your order №$orderId is cooked.")
        }
    }
}
