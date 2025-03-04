#!/bin/bash

dotnet publish --os linux --arch x64 /t:PublishContainer -p ContainerArchiveOutputPath=PayamaX.Portal.tar.gz
docker load -i ./PayamaX.Portal.tar.gz
rm -f PayamaX.Portal.tar.gz
