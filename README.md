This is just a minimal PoC.

Argo is deployed and configured to automatically deploy any changes to YAML files in k8s/-folder (via both webhook and polling), as well as the Docker images built and pushed to the private registry under the GitHub action that triggers on commit.
