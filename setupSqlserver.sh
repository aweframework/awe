#!/bin/bash

sudo curl https://packages.microsoft.com/keys/microsoft.asc | apt-key add -

#Download appropriate package for the OS version
#Ubuntu 20.04
sudo add-apt-repository "$(curl https://packages.microsoft.com/config/ubuntu/20.04/prod.list)"
sudo apt-get update
echo 'export PATH="$PATH:/opt/mssql-tools/bin"' >> ~/.bashrc
source ~/.bashrc
# optional: for unixODBC development headers
sudo apt-get install -y mssql-tools