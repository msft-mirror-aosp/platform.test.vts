#
# Copyright (C) 2017 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
"""Class to flash build artifacts onto devices"""

import logging
import os

from vts.harnesses.host_controller.build import build_provider
from vts.utils.python.controllers import android_device


class BuildFlasher(object):
    """Client that manages build flashing.

    Attributes:
        device: AndroidDevice, the device associated with the client.
    """

    def __init__(self, serial=""):
        """Initialize the client.

        If serial not provided, find single device connected. Error if
        zero or > 1 devices connected.

        Args:
            serial: optional string, serial number for the device.
        """
        if serial != "":
            self.device = android_device.AndroidDevice(serial)
        else:
            serials = android_device.list_adb_devices()
            if len(serials) == 0:
                serials = android_device.list_fastboot_devices()
                if len(serials) == 0:
                    raise android_device.AndroidDeviceError(
                        "ADB and fastboot could not find any target devices.")
            if len(serials) > 1:
                print("ADB or fastboot found more than one device: %s" % serials)
            self.device = android_device.AndroidDevice(
                serials[0], device_callback_port=-1)

    def SetSerial(self, serial):
        """Sets device serial.

        Args:
            serial: string, a device serial.

        Returns:
            True if successful; False otherwise.
        """
        if not serial:
            print("no serial is given to BuildFlasher.SetSerial.")
            return False

        self.device = android_device.AndroidDevice(serial)
        return True

    def FlashGSI(self, system_img, vbmeta_img=None, skip_check=False):
        """Flash the Generic System Image to the device.

        Args:
            system_img: string, path to GSI
            vbmeta_img: string, optional, path to vbmeta image for new devices
            skip_check: boolean, set True to skip adb-based checks when
                        the DUT is already running its bootloader.
        """
        if not os.path.exists(system_img):
            raise ValueError("Couldn't find system image at %s" % system_img)
        if not skip_check:
            self.device.adb.wait_for_device()
            if not self.device.isBootloaderMode:
                self.device.log.info(self.device.adb.reboot_bootloader())
        if vbmeta_img is not None:
            self.device.log.info(self.device.fastboot.flash(
                'vbmeta', vbmeta_img))
        self.device.log.info(self.device.fastboot.erase('system'))
        self.device.log.info(self.device.fastboot.flash('system', system_img))
        self.device.log.info(self.device.fastboot.erase('metadata'))
        self.device.log.info(self.device.fastboot._w())
        self.device.log.info(self.device.fastboot.reboot())

    def Flashall(self, directory):
        """Flash all images in a directory to the device using flashall.

        Generally the directory is the result of unzipping the .zip from AB.
        Args:
            directory: string, path to directory containing images
        """
        # fastboot flashall looks for imgs in $ANDROID_PRODUCT_OUT
        os.environ['ANDROID_PRODUCT_OUT'] = directory
        self.device.adb.wait_for_device()
        if not self.device.isBootloaderMode:
            self.device.log.info(self.device.adb.reboot_bootloader())
        self.device.log.info(self.device.fastboot.flashall())

    def Flash(self, device_images):
        """Flash the Generic System Image to the device.

        Args:
            device_images: dict, where the key is partition name and value is
                           image file path.

        Returns:
            True if succesful; False otherwise
        """
        if not device_images:
            logging.warn("Flash skipped because no device image is given.")
            return False

        if not self.device.isBootloaderMode:
            self.device.adb.wait_for_device()
            print("rebooting to bootloader")
            self.device.log.info(self.device.adb.reboot_bootloader())

        print("starting to flash vendor and other images...")
        if build_provider.FULL_ZIPFILE in device_images:
            print("fastboot update %s --skip-reboot" % (
                  device_images[build_provider.FULL_ZIPFILE]))
            self.device.log.info(self.device.fastboot.update(
                device_images[build_provider.FULL_ZIPFILE],
                "--skip-reboot"))

        for partition, image_path in device_images.iteritems():
            if partition in (build_provider.FULL_ZIPFILE, "system", "vbmeta"):
                continue
            if not image_path:
                self.device.log.warning("%s image is empty", partition)
                continue
            self.device.log.info("fastboot flash %s %s", partition, image_path)
            self.device.log.info(
                self.device.fastboot.flash(partition, image_path))

        print("starting to flash system and other images...")
        if "system" in device_images and device_images["system"]:
            system_img = device_images["system"]
            vbmeta_img = device_images["vbmeta"] if (
                "vbmeta" in device_images and device_images["vbmeta"]) else None
            self.FlashGSI(system_img, vbmeta_img, skip_check=True)
        else:
            self.device.log.info(self.device.fastboot.reboot())
        return True

    def WaitForDevice(self, timeout_secs=600):
        """Waits for the device to boot completely.

        Args:
            timeout_secs: integer, the maximum timeout value for this
                          operation (unit: seconds).

        Returns:
            True if device is booted successfully; False otherwise.
        """
        return self.device.waitForBootCompletion(timeout=timeout_secs)
