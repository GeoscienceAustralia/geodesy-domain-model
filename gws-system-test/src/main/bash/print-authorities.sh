#!/usr/bin/env bash

set -e

openam=http://localhost:8083/openam

clientId=GnssSiteManager
clientPassword=

username=
password=

jwt=$(curl -s --user ${clientId}:${clientPassword} --data "grant_type=password&username=${username}&password=${password}&scope=openid profile" ${openam}/oauth2/access_token?realm=/ | jq .id_token | tr -d '"')

echo -n ${jwt} | cut -f2 -d. | base64 -d | jq .authorities
