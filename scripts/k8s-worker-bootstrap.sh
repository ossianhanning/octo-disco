#!/bin/bash
set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

if [ "$EUID" -ne 0 ]; then 
    echo "Please run as root (use sudo)"
    exit 1
fi

echo "1) Control Plane (master node)"
echo "2) Worker Node"
read -p "Enter choice [1-2]: " NODE_TYPE_CHOICE

case $NODE_TYPE_CHOICE in
    1)
        NODE_TYPE="control-plane"
        echo -e "${GREEN}Selected: Control Plane${NC}"
        ;;
    2)
        NODE_TYPE="worker"
        echo -e "${GREEN}Selected: Worker Node${NC}"
        ;;
    *)
        echo "Invalid choice. Exiting."
        exit 1
        ;;
esac

echo ""

if [ "$NODE_TYPE" == "control-plane" ]; then
    read -p "Enter control plane IP address: " CONTROL_PLANE_IP
    if [ -z "$CONTROL_PLANE_IP" ]; then
        exit 1
    fi
    
    read -p "Enter pod network CIDR [default: 10.244.0.0/16]: " POD_CIDR
    POD_CIDR=${POD_CIDR:-10.244.0.0/16}
fi


swapoff -a
sed -i '/ swap / s/^/#/' /etc/fstab
cat <<EOF | tee /etc/sysctl.d/k8s.conf
net.ipv4.ip_forward = 1
EOF
sysctl --system

apt-get update
apt-get install -y apt-transport-https ca-certificates curl gpg


install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc

tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/debian
Suites: $(. /etc/os-release && echo "$VERSION_CODENAME")
Components: stable
Signed-By: /etc/apt/keyrings/docker.asc
EOF

apt-get update
apt-get install -y containerd.io

mkdir -p /etc/containerd
containerd config default | tee /etc/containerd/config.toml

sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml

systemctl restart containerd
systemctl enable containerd

curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.35/deb/Release.key | gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg
echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.35/deb/ /' | tee /etc/apt/sources.list.d/kubernetes.list

apt-get update
apt-get install -y kubelet kubeadm kubectl
apt-mark hold kubelet kubeadm kubectl

systemctl enable --now kubelet

if [ "$NODE_TYPE" == "control-plane" ]; then
    kubeadm init \
        --apiserver-advertise-address="$CONTROL_PLANE_IP" \
        --pod-network-cidr="$POD_CIDR" \
        --skip-phases=addon/kube-proxy
    
    mkdir -p /root/.kube
    cp -f /etc/kubernetes/admin.conf /root/.kube/config
    chown root:root /root/.kube/config
    
    if [ -n "$SUDO_USER" ]; then
        SUDO_HOME=$(eval echo ~$SUDO_USER)
        mkdir -p "$SUDO_HOME/.kube"
        cp -f /etc/kubernetes/admin.conf "$SUDO_HOME/.kube/config"
        chown -R $SUDO_USER:$SUDO_USER "$SUDO_HOME/.kube"
    fi
    
    CILIUM_CLI_VERSION=$(curl -s https://raw.githubusercontent.com/cilium/cilium-cli/main/stable.txt)
    CLI_ARCH=amd64
    if [ "$(uname -m)" = "aarch64" ]; then CLI_ARCH=arm64; fi
    
    curl -L --fail --remote-name-all https://github.com/cilium/cilium-cli/releases/download/${CILIUM_CLI_VERSION}/cilium-linux-${CLI_ARCH}.tar.gz{,.sha256sum}
    sha256sum --check cilium-linux-${CLI_ARCH}.tar.gz.sha256sum
    tar xzvf cilium-linux-${CLI_ARCH}.tar.gz -C /usr/local/bin
    rm cilium-linux-${CLI_ARCH}.tar.gz{,.sha256sum}
    
    if [ -n "$SUDO_USER" ]; then
        su - $SUDO_USER -c "cilium install --version 1.15.1"
    else
        cilium install --version 1.15.1
    fi
fi
