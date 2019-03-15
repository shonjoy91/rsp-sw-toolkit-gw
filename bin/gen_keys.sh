#!/bin/bash

#----------------------------------------------------------------
#-- create an openssl.cfg for this CA used when signing the CSR
#----------------------------------------------------------------
cat > ca.conf <<__EOF__
[ ca ]
default_ca = PlatformRootCA

[ PlatformRootCA ]
unique_subject = no
new_certs_dir = .
certificate = ca.crt
database = certindex
private_key = ca.key
serial = serial
default_days = 365
default_md = sha1
policy = PlatformRootCA_policy
x509_extensions = PlatformRootCA_extensions

[ PlatformRootCA_policy ]
commonName = supplied
stateOrProvinceName = supplied
countryName = supplied
emailAddress = optional
organizationName = supplied
organizationalUnitName = optional

[ PlatformRootCA_extensions ]
basicConstraints = CA:false
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always
keyUsage = digitalSignature,keyEncipherment
extendedKeyUsage = serverAuth,clientAuth

__EOF__

#----------------------------------------------------------------
#-- Variables used 
#----------------------------------------------------------------
COUNTRY="US"
STATE="Arizona"
LOCALITY="Chandler"
ORGANIZATION="Ace Point Inc."
# make sure this matches the server hostname that will be used 
# for the gateway REST functionality. By default, it will be
# the MDNS name of the server
SERVER_FQDN="${HOSTNAME}.local"

# TODO: check the environment for these, then use a default?
CA_KEY_PASSWORD="ca123key456"
SERVER_KEY_PASSWORD="server123key456"
SERVER_STORE_PASSWORD="server123store456"
#----------------------------------------------------------------

# this file tracks the next available certficate serial number
if [ ! -f serial ]; then
    echo "0001" > serial
fi

# The CA records the certificates it signs here
touch certindex

#----------------------------------------------------------------
#-- Create CA certificate
#    -nodes \
#    -passin pass:${CA_KEY_PASSWORD} \
openssl req \
    -newkey rsa:4096 \
    -keyout ca.key \
    -nodes \
    -x509 -days 365  -out ca.crt \
    -subj "/C=${COUNTRY}/ST=${STATE}/L=${LOCALITY}/O=${ORGANIZATION}/CN=PlatformRootCA"

#----------------------------------------------------------------
#-- Create a CSR - certificate signing request
#    -nodes \
#    -passin pass:${SERVER_KEY_PASSWORD} \
openssl req \
    -newkey rsa:1024 \
    -keyout server.key \
    -nodes \
    -out server.csr \
    -subj "/C=${COUNTRY}/ST=${STATE}/L=${LOCALITY}/O=${ORGANIZATION}/CN=${SERVER_FQDN}"

#----------------------------------------------------------------
#-- Sign the CSR
# Produces a certificate (-out) 
# Createis new versions of serial and certindex files, 
# moving the old versions to backup files. 
# Creates <cert-serial-num>.pem file - identical to <-out>
# -batch: certifies any CSRs passed in without prompting.
openssl ca -batch -config ca.conf -notext -in server.csr -out server.crt

#----------------------------------------------------------------
#-- create a pkcs12 thingamajiggy
#  -passin pass:password_for_key -passout pass:password_for_pkcs12_cert
openssl pkcs12 \
    -inkey server.key -in server.crt \
    -export -passout pass:${SERVER_STORE_PASSWORD} -out server.p12

#----------------------------------------------------------------
#-- import pkcs12 store into java keystore format 
keytool -importkeystore \
    -srckeystore server.p12     -srcstoretype PKCS12 -srcstorepass ${SERVER_STORE_PASSWORD} \
    -destkeystore keystore.p12 -deststoretype PKCS12 -deststorepass ${SERVER_STORE_PASSWORD}

#    -keypass "" \
#keytool -importcert \
#    -storepass abc123 \
#    -trustcacerts \
#    -noprompt \
#    -file server.pkcs12 \
#    -alias ${SERVER_FQDN} \
#    -keystore keystore.jks \

