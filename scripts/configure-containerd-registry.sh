#!/bin/bash

sudo mkdir -p /etc/containerd/certs.d/100.76.133.85:30500
sudo tee /etc/containerd/certs.d/100.76.133.85:30500/hosts.toml > /dev/null << 'EOF'
server = "http://100.76.133.85:30500"

[host."http://100.76.133.85:30500"]
  capabilities = ["pull", "resolve"]
EOF

sudo sed -i '/^\[plugins\."io\.containerd\.grpc\.v1\.cri"\.registry\.mirrors/,/^$/d' /etc/containerd/config.toml
sudo sed -i '/^\[plugins\."io\.containerd\.grpc\.v1\.cri"\.registry\.configs/,/^$/d' /etc/containerd/config.toml
sudo sed -i '/^# Private registry configuration/,/insecure_skip_verify = true$/d' /etc/containerd/config.toml

if grep -q '^\[plugins\."io\.containerd\.grpc\.v1\.cri"\.registry\]' /etc/containerd/config.toml; then
  sudo sed -i '/^\[plugins\."io\.containerd\.grpc\.v1\.cri"\.registry\]/a\  config_path = "/etc/containerd/certs.d"' /etc/containerd/config.toml
  sudo sed -i '/^\[plugins\."io\.containerd\.grpc\.v1\.cri"\.registry\]/,/^\[/{/config_path/!b;n;:a;/config_path/d;n;ba}' /etc/containerd/config.toml
else
  sudo tee -a /etc/containerd/config.toml > /dev/null << 'EOF'

[plugins."io.containerd.grpc.v1.cri".registry]
  config_path = "/etc/containerd/certs.d"
EOF
Fix
sudo systemctl restart containerd
sudo systemctl restart kubelet
sleep 5

if sudo crictl pull 100.76.133.85:30500/event-ingester:latest 2>&1 | grep -q 'Image is up to date\|Pulling from\|pulled'; then
  echo "SUCCESS"
else
  echo "FAILURE"
fi
