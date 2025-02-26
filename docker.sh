podman build -t payamax.portal -f Podman.docker -v /home/hassanbehzadian/nuget/local:/nuget .
podman save -o PayamaX.Portal.tar payamax.portal:latest
docker load -i PayamaX.Portal.tar
rm PayamaX.Portal.tar