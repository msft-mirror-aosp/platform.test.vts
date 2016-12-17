#include "hardware/interfaces/nfc/1.0/vts/Nfc.vts.h"
#include "vts_datatype.h"
#include "vts_measurement.h"
#include <iostream>
#include <hidl/HidlSupport.h>
#include <android/hardware/nfc/1.0/INfc.h>
#include <android/hardware/nfc/1.0/INfcClientCallback.h>
#include "hardware/interfaces/nfc/1.0/vts/NfcClientCallback.vts.h"
#include "hardware/interfaces/nfc/1.0/vts/types.vts.h"
#include <android/hardware/nfc/1.0/types.h>


using namespace android::hardware::nfc::V1_0;
namespace android {
namespace vts {
static void FuzzerExtended_INfcopen_cb_func(::android::hardware::nfc::V1_0::NfcStatus arg0) {
  cout << "callback open called" << endl;
}
std::function<void(::android::hardware::nfc::V1_0::NfcStatus)> FuzzerExtended_INfcopen_cb = FuzzerExtended_INfcopen_cb_func;


static void FuzzerExtended_INfcwrite_cb_func(uint32_t arg0) {
  cout << "callback write called" << endl;
}
std::function<void(uint32_t)> FuzzerExtended_INfcwrite_cb = FuzzerExtended_INfcwrite_cb_func;


static void FuzzerExtended_INfccoreInitialized_cb_func(::android::hardware::nfc::V1_0::NfcStatus arg0) {
  cout << "callback coreInitialized called" << endl;
}
std::function<void(::android::hardware::nfc::V1_0::NfcStatus)> FuzzerExtended_INfccoreInitialized_cb = FuzzerExtended_INfccoreInitialized_cb_func;


static void FuzzerExtended_INfcprediscover_cb_func(::android::hardware::nfc::V1_0::NfcStatus arg0) {
  cout << "callback prediscover called" << endl;
}
std::function<void(::android::hardware::nfc::V1_0::NfcStatus)> FuzzerExtended_INfcprediscover_cb = FuzzerExtended_INfcprediscover_cb_func;


static void FuzzerExtended_INfcclose_cb_func(::android::hardware::nfc::V1_0::NfcStatus arg0) {
  cout << "callback close called" << endl;
}
std::function<void(::android::hardware::nfc::V1_0::NfcStatus)> FuzzerExtended_INfcclose_cb = FuzzerExtended_INfcclose_cb_func;


static void FuzzerExtended_INfccontrolGranted_cb_func(::android::hardware::nfc::V1_0::NfcStatus arg0) {
  cout << "callback controlGranted called" << endl;
}
std::function<void(::android::hardware::nfc::V1_0::NfcStatus)> FuzzerExtended_INfccontrolGranted_cb = FuzzerExtended_INfccontrolGranted_cb_func;


static void FuzzerExtended_INfcpowerCycle_cb_func(::android::hardware::nfc::V1_0::NfcStatus arg0) {
  cout << "callback powerCycle called" << endl;
}
std::function<void(::android::hardware::nfc::V1_0::NfcStatus)> FuzzerExtended_INfcpowerCycle_cb = FuzzerExtended_INfcpowerCycle_cb_func;


bool FuzzerExtended_INfc::GetService(bool get_stub, const char* service_name) {
    static bool initialized = false;
    if (!initialized) {
        cout << "[agent:hal] HIDL getService" << endl;
        if (service_name) {
          cout << "  - service name: " << service_name << endl;
        }
        hw_binder_proxy_ = INfc::getService(service_name, get_stub);
        cout << "[agent:hal] hw_binder_proxy_ = " << hw_binder_proxy_.get() << endl;
        initialized = true;
    }
    return true;
}

bool FuzzerExtended_INfc::Fuzz(
    FunctionSpecificationMessage* func_msg,
    void** result, const string& callback_socket_name) {
    const char* func_name = func_msg->name().c_str();
    cout << "Function: " << __func__ << " " << func_name << endl;
    if (!strcmp(func_name, "open")) {
        sp<INfcClientCallback> arg0(VtsFuzzerCreateINfcClientCallback(callback_socket_name));
        VtsMeasurement vts_measurement;
        vts_measurement.Start();
        cout << "Call an API" << endl;
        cout << "local_device = " << hw_binder_proxy_.get();
            *result = reinterpret_cast<void*>((::android::hardware::nfc::V1_0::NfcStatus)hw_binder_proxy_->open(
            arg0));
        vector<float>* measured = vts_measurement.Stop();
        cout << "time " << (*measured)[0] << endl;
        cout << "called" << endl;
        return true;
    }
    if (!strcmp(func_name, "write")) {
        uint8_t* arg0buffer = (uint8_t*) malloc(func_msg->arg(0).vector_size() * sizeof(uint8_t));
        ::android::hardware::hidl_vec<uint8_t> arg0;
        for (int vector_index = 0; vector_index < func_msg->arg(0).vector_size(); vector_index++) {
            arg0buffer[vector_index] = func_msg->arg(0).vector_value(vector_index).scalar_value().uint8_t();
        }
        arg0.setToExternal(arg0buffer, func_msg->arg(0).vector_size());
        VtsMeasurement vts_measurement;
        vts_measurement.Start();
        cout << "Call an API" << endl;
        cout << "local_device = " << hw_binder_proxy_.get();
        *result = reinterpret_cast<void*>((uint32_t)hw_binder_proxy_->write(
            arg0));
        vector<float>* measured = vts_measurement.Stop();
        cout << "time " << (*measured)[0] << endl;
        cout << "called" << endl;
        return true;
    }
    if (!strcmp(func_name, "coreInitialized")) {
        uint8_t* arg0buffer = (uint8_t*) malloc(func_msg->arg(0).vector_size() * sizeof(uint8_t));
        ::android::hardware::hidl_vec<uint8_t> arg0;
        for (int vector_index = 0; vector_index < func_msg->arg(0).vector_size(); vector_index++) {
            arg0buffer[vector_index] = func_msg->arg(0).vector_value(vector_index).scalar_value().uint8_t();
        }
        arg0.setToExternal(arg0buffer, func_msg->arg(0).vector_size());
        VtsMeasurement vts_measurement;
        vts_measurement.Start();
        cout << "Call an API" << endl;
        cout << "local_device = " << hw_binder_proxy_.get();
            *result = reinterpret_cast<void*>((::android::hardware::nfc::V1_0::NfcStatus)hw_binder_proxy_->coreInitialized(
            arg0));
        vector<float>* measured = vts_measurement.Stop();
        cout << "time " << (*measured)[0] << endl;
        cout << "called" << endl;
        return true;
    }
    if (!strcmp(func_name, "prediscover")) {
        VtsMeasurement vts_measurement;
        vts_measurement.Start();
        cout << "Call an API" << endl;
        cout << "local_device = " << hw_binder_proxy_.get();
            *result = reinterpret_cast<void*>((::android::hardware::nfc::V1_0::NfcStatus)hw_binder_proxy_->prediscover());
        vector<float>* measured = vts_measurement.Stop();
        cout << "time " << (*measured)[0] << endl;
        cout << "called" << endl;
        return true;
    }
    if (!strcmp(func_name, "close")) {
        VtsMeasurement vts_measurement;
        vts_measurement.Start();
        cout << "Call an API" << endl;
        cout << "local_device = " << hw_binder_proxy_.get();
            *result = reinterpret_cast<void*>((::android::hardware::nfc::V1_0::NfcStatus)hw_binder_proxy_->close());
        vector<float>* measured = vts_measurement.Stop();
        cout << "time " << (*measured)[0] << endl;
        cout << "called" << endl;
        return true;
    }
    if (!strcmp(func_name, "controlGranted")) {
        VtsMeasurement vts_measurement;
        vts_measurement.Start();
        cout << "Call an API" << endl;
        cout << "local_device = " << hw_binder_proxy_.get();
            *result = reinterpret_cast<void*>((::android::hardware::nfc::V1_0::NfcStatus)hw_binder_proxy_->controlGranted());
        vector<float>* measured = vts_measurement.Stop();
        cout << "time " << (*measured)[0] << endl;
        cout << "called" << endl;
        return true;
    }
    if (!strcmp(func_name, "powerCycle")) {
        VtsMeasurement vts_measurement;
        vts_measurement.Start();
        cout << "Call an API" << endl;
        cout << "local_device = " << hw_binder_proxy_.get();
            *result = reinterpret_cast<void*>((::android::hardware::nfc::V1_0::NfcStatus)hw_binder_proxy_->powerCycle());
        vector<float>* measured = vts_measurement.Stop();
        cout << "time " << (*measured)[0] << endl;
        cout << "called" << endl;
        return true;
    }
    return false;
}
bool FuzzerExtended_INfc::GetAttribute(
    FunctionSpecificationMessage* func_msg,
    void** result) {
  cerr << "attribute not found" << endl;
  return false;
}
bool FuzzerExtended_INfc::CallFunction(FunctionSpecificationMessage* func_msg, void** result, const string& callback_socket_name) {
    const char* func_name = func_msg->name().c_str();
    cout << "Function: " << __func__ << " " << func_name << endl;
    if (!strcmp(func_name, "open")) {
        sp<INfcClientCallback> arg0;
        arg0 = VtsFuzzerCreateINfcClientCallback(callback_socket_name);
        VtsMeasurement vts_measurement;
        vts_measurement.Start();
        cout << "Call an API" << endl;
        cout << "local_device = " << hw_binder_proxy_.get();
            *result = reinterpret_cast<void*>((::android::hardware::nfc::V1_0::NfcStatus)hw_binder_proxy_->open(
            arg0));
        vector<float>* measured = vts_measurement.Stop();
        cout << "time " << (*measured)[0] << endl;
        cout << "called" << endl;
        return true;
    }
    if (!strcmp(func_name, "write")) {
        ::android::hardware::hidl_vec<uint8_t> arg0;
        arg0.resize(func_msg->arg(0).vector_size());
        for (int i = 0; i <func_msg->arg(0).vector_size(); i++) {
            arg0[i] = func_msg->arg(0).vector_value(i).scalar_value().uint8_t();
        }
        VtsMeasurement vts_measurement;
        vts_measurement.Start();
        cout << "Call an API" << endl;
        cout << "local_device = " << hw_binder_proxy_.get();
        *result = reinterpret_cast<void*>((uint32_t)hw_binder_proxy_->write(
            arg0));
        vector<float>* measured = vts_measurement.Stop();
        cout << "time " << (*measured)[0] << endl;
        cout << "called" << endl;
        return true;
    }
    if (!strcmp(func_name, "coreInitialized")) {
        ::android::hardware::hidl_vec<uint8_t> arg0;
        arg0.resize(func_msg->arg(0).vector_size());
        for (int i = 0; i <func_msg->arg(0).vector_size(); i++) {
            arg0[i] = func_msg->arg(0).vector_value(i).scalar_value().uint8_t();
        }
        VtsMeasurement vts_measurement;
        vts_measurement.Start();
        cout << "Call an API" << endl;
        cout << "local_device = " << hw_binder_proxy_.get();
            *result = reinterpret_cast<void*>((::android::hardware::nfc::V1_0::NfcStatus)hw_binder_proxy_->coreInitialized(
            arg0));
        vector<float>* measured = vts_measurement.Stop();
        cout << "time " << (*measured)[0] << endl;
        cout << "called" << endl;
        return true;
    }
    if (!strcmp(func_name, "prediscover")) {
        VtsMeasurement vts_measurement;
        vts_measurement.Start();
        cout << "Call an API" << endl;
        cout << "local_device = " << hw_binder_proxy_.get();
            *result = reinterpret_cast<void*>((::android::hardware::nfc::V1_0::NfcStatus)hw_binder_proxy_->prediscover());
        vector<float>* measured = vts_measurement.Stop();
        cout << "time " << (*measured)[0] << endl;
        cout << "called" << endl;
        return true;
    }
    if (!strcmp(func_name, "close")) {
        VtsMeasurement vts_measurement;
        vts_measurement.Start();
        cout << "Call an API" << endl;
        cout << "local_device = " << hw_binder_proxy_.get();
            *result = reinterpret_cast<void*>((::android::hardware::nfc::V1_0::NfcStatus)hw_binder_proxy_->close());
        vector<float>* measured = vts_measurement.Stop();
        cout << "time " << (*measured)[0] << endl;
        cout << "called" << endl;
        return true;
    }
    if (!strcmp(func_name, "controlGranted")) {
        VtsMeasurement vts_measurement;
        vts_measurement.Start();
        cout << "Call an API" << endl;
        cout << "local_device = " << hw_binder_proxy_.get();
            *result = reinterpret_cast<void*>((::android::hardware::nfc::V1_0::NfcStatus)hw_binder_proxy_->controlGranted());
        vector<float>* measured = vts_measurement.Stop();
        cout << "time " << (*measured)[0] << endl;
        cout << "called" << endl;
        return true;
    }
    if (!strcmp(func_name, "powerCycle")) {
        VtsMeasurement vts_measurement;
        vts_measurement.Start();
        cout << "Call an API" << endl;
        cout << "local_device = " << hw_binder_proxy_.get();
            *result = reinterpret_cast<void*>((::android::hardware::nfc::V1_0::NfcStatus)hw_binder_proxy_->powerCycle());
        vector<float>* measured = vts_measurement.Stop();
        cout << "time " << (*measured)[0] << endl;
        cout << "called" << endl;
        return true;
    }
    return false;
}

bool FuzzerExtended_INfc::VerifyResults(FunctionSpecificationMessage* verification_msg, vector<void *>results) {
    if (!strcmp(verification_msg->name().c_str(), "open")) {
        if (results.size() != 1) { return false; }
        ::android::hardware::nfc::V1_0::NfcStatus *result_0 = reinterpret_cast<::android::hardware::nfc::V1_0::NfcStatus*> (results[0]);
        if(!Verify__android__hardware__nfc__V1_0__NfcStatus((*result_0), verification_msg->return_type_hidl(0))) { return false; }
    }
    if (!strcmp(verification_msg->name().c_str(), "write")) {
        if (results.size() != 1) { return false; }
        uint32_t *result_0 = reinterpret_cast<uint32_t*> (results[0]);
        if((*result_0)!= verification_msg->return_type_hidl(0).scalar_value().uint32_t()) { return false; }
    }
    if (!strcmp(verification_msg->name().c_str(), "coreInitialized")) {
        if (results.size() != 1) { return false; }
        ::android::hardware::nfc::V1_0::NfcStatus *result_0 = reinterpret_cast<::android::hardware::nfc::V1_0::NfcStatus*> (results[0]);
        if(!Verify__android__hardware__nfc__V1_0__NfcStatus((*result_0), verification_msg->return_type_hidl(0))) { return false; }
    }
    if (!strcmp(verification_msg->name().c_str(), "prediscover")) {
        if (results.size() != 1) { return false; }
        ::android::hardware::nfc::V1_0::NfcStatus *result_0 = reinterpret_cast<::android::hardware::nfc::V1_0::NfcStatus*> (results[0]);
        if(!Verify__android__hardware__nfc__V1_0__NfcStatus((*result_0), verification_msg->return_type_hidl(0))) { return false; }
    }
    if (!strcmp(verification_msg->name().c_str(), "close")) {
        if (results.size() != 1) { return false; }
        ::android::hardware::nfc::V1_0::NfcStatus *result_0 = reinterpret_cast<::android::hardware::nfc::V1_0::NfcStatus*> (results[0]);
        if(!Verify__android__hardware__nfc__V1_0__NfcStatus((*result_0), verification_msg->return_type_hidl(0))) { return false; }
    }
    if (!strcmp(verification_msg->name().c_str(), "controlGranted")) {
        if (results.size() != 1) { return false; }
        ::android::hardware::nfc::V1_0::NfcStatus *result_0 = reinterpret_cast<::android::hardware::nfc::V1_0::NfcStatus*> (results[0]);
        if(!Verify__android__hardware__nfc__V1_0__NfcStatus((*result_0), verification_msg->return_type_hidl(0))) { return false; }
    }
    if (!strcmp(verification_msg->name().c_str(), "powerCycle")) {
        if (results.size() != 1) { return false; }
        ::android::hardware::nfc::V1_0::NfcStatus *result_0 = reinterpret_cast<::android::hardware::nfc::V1_0::NfcStatus*> (results[0]);
        if(!Verify__android__hardware__nfc__V1_0__NfcStatus((*result_0), verification_msg->return_type_hidl(0))) { return false; }
    }
    return true;
}

extern "C" {
android::vts::FuzzerBase* vts_func_4_android_hardware_nfc_1_() {
    return (android::vts::FuzzerBase*) new android::vts::FuzzerExtended_INfc();
}

}
}  // namespace vts
}  // namespace android
