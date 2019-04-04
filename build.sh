#!/bin/bash
clj -A:pack mach.pack.alpha.capsule terraform.jar --application-id terraform --application-version "$(git describe)" -m terraform
