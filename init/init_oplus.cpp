/*
 * Copyright (C) 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

#include <android-base/file.h>
#include <android-base/logging.h>
#include <android-base/properties.h>
#include <android-base/strings.h>

#define _REALLY_INCLUDE_SYS__SYSTEM_PROPERTIES_H_
#include <sys/_system_properties.h>

using android::base::GetProperty;
using android::base::ReadFileToString;
using android::base::Split;
using android::base::Trim;

/*
 * SetProperty does not allow updating read only properties and as a result
 * does not work for our use case. Write "OverrideProperty" to do practically
 * the same thing as "SetProperty" without this restriction.
 */
void OverrideProperty(const char* name, const char* value) {
    size_t valuelen = strlen(value);

    prop_info* pi = (prop_info*)__system_property_find(name);
    if (pi != nullptr) {
        __system_property_update(pi, value, valuelen);
    } else {
        __system_property_add(name, strlen(name), value, valuelen);
    }
}

/*
 * Only for read-only properties. Properties that can be wrote to more
 * than once should be set in a typical init script (e.g. init.oplus.hw.rc)
 * after the original property has been set.
 */
void vendor_load_properties() {
    auto prjname_string = GetProperty("ro.boot.prjname", "0");
    int prjname = 0;
    char* end;
    long val;

    if (prjname_string.find_first_of("AB") != std::string::npos) { // for specific prjname string(2161A, 2169A, 2169B)
        val = strtol(prjname_string.c_str(), &end, 16);
    } else {
        val = strtol(prjname_string.c_str(), &end, 10);
    }

    if (*end != '\0') {
        LOG(ERROR) << "Invalid project name format: " << prjname_string;
        return;
    }

    prjname = static_cast<int>(val);

    switch (prjname) {
        case 21619: // bitra CN
        case 0x2161A: // bitra CN (Dragon Ball Edition)
            OverrideProperty("ro.product.product.model", "RMX3370");
            OverrideProperty("ro.product.product.device", "RE5473");
            if (prjname == 21619) {
                OverrideProperty("ro.product.marketname", "realme GT Neo2");
            } else {
                OverrideProperty("ro.product.marketname", "realme GT Neo2 Dragon Ball Edition");
            }
            break;
        case 0x2169A: // bitra IN
        case 0x2169B: // bitra EU
            OverrideProperty("ro.product.product.model", "RMX3370");
            OverrideProperty("ro.product.product.device", "RE879AL1");
            OverrideProperty("ro.product.marketname", "realme GT NEO 2");
            break;
        case 21623: // spartan CN
            OverrideProperty("ro.product.product.model", "RMX3372");
            OverrideProperty("ro.product.product.device", "RE5477");
            OverrideProperty("ro.product.marketname", "realme Q5 Pro");
            break;
        case 21732: // spartan IN
        case 21733: // spartan EU
            OverrideProperty("ro.product.product.model", "RMX3371");
            OverrideProperty("ro.product.product.device", "RE54E4L1");
            OverrideProperty("ro.product.marketname", "realme GT NEO 3T");
            break;
        default:
            LOG(ERROR) << "Unexpected project name: " << prjname;
    }

    // Disable NFC for Q5 Pro(CN) / GT NEO 3T(IN)
    if (prjname != 21623 && prjname != 21732) {
        OverrideProperty("ro.boot.product.hardware.sku", "nfc");
    }

    if (std::string content; ReadFileToString("/proc/devinfo/ddr_type", &content)) {
        OverrideProperty("ro.boot.ddr_type", Split(Trim(content), "\t").back().c_str());
    }
}
