package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDao
import com.example.data.repository.WassalniRepository
import kotlinx.coroutines.launch
import java.util.UUID

class AppViewModel(
    private val repository: WassalniRepository,
    private val dao: AppDao
) : ViewModel() {

    // ... (بقية الدوال الموجودة سابقاً)

    // تحديث دالة الموافقة لتشمل الحذف المحلي والمزامنة
    fun approveTopUpRequest(requestId: String) {
        viewModelScope.launch {
            val result = repository.adminApproveTopUp(requestId)
            if (result.isSuccess) {
                // حذف الطلب محلياً ليختفي من القائمة
                dao.deleteTopUpRequest(requestId)
                // مزامنة البيانات المتبقية
                repository.syncAdminTopUpRequests()
                addAdminActivityLog("موافقة شحن نقاط", "الموافقة على طلب الشحن $requestId")
            } else {
                println("ERROR: Failed to approve top-up $requestId: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    // تحديث دالة رفض الطلب لتشمل الحذف المحلي
    fun rejectTopUpRequest(requestId: String, reason: String) {
        viewModelScope.launch {
            val result = repository.adminRejectTopUp(requestId, reason)
            if (result.isSuccess) {
                dao.deleteTopUpRequest(requestId)
                addAdminActivityLog("رفض شحن نقاط", "رفض طلب $requestId بسبب: $reason")
            }
        }
    }

    // إضافة دالة مسح جميع الإشعارات للمستخدم
    fun clearAllNotifications(userId: String) {
        viewModelScope.launch {
            dao.clearUserNotifications(userId)
        }
    }

    // باقي الدوال...
    // تأكد من ترك باقي الدوال الأصلية كما هي في ملفك الحالي
}
