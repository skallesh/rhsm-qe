#!/bin/bash
/usr/bin/dbus-monitor --system --profile "interface='com.redhat.SubscriptionManager.EntitlementStatus',member='entitlement_status_changed'"
