# Build the Podman image
podman build -t payamax.portal -f Podman.docker --volume "$HOME/nuget/repo/local:/nuget" .

# Save the image to a tar file
podman save -o PayamaX.Portal.tar payamax.portal:latest

# Load the image into Docker
docker load -i PayamaX.Portal.tar

# Remove the tar file
Remove-Item -Path "PayamaX.Portal.tar" -Force
