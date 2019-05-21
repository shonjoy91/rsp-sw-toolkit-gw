@ECHO OFF

:: Create an openssl.cfg for this CA used when signing the CSR
echo [ ca ] > ca.conf
echo default_ca = PlatformRootCA >> ca.conf
echo. >> ca.conf
echo [ PlatformRootCA ] >> ca.conf
echo unique_subject = no >> ca.conf
echo new_certs_dir = . >> ca.conf
echo certificate = ca.crt >> ca.conf
echo database = certindex >> ca.conf
echo private_key = ca.key >> ca.conf
echo serial = serial >> ca.conf
echo default_days = 365 >> ca.conf
echo default_md = sha1 >> ca.conf
echo policy = PlatformRootCA_policy >> ca.conf
echo x509_extensions = PlatformRootCA_extensions >> ca.conf
echo. >> ca.conf
echo [ PlatformRootCA_policy ] >> ca.conf
echo commonName = supplied >> ca.conf
echo stateOrProvinceName = supplied >> ca.conf
echo countryName = supplied >> ca.conf
echo emailAddress = optional >> ca.conf
echo organizationName = supplied >> ca.conf
echo organizationalUnitName = optional >> ca.conf
echo. >> ca.conf
echo [ PlatformRootCA_extensions ] >> ca.conf
echo basicConstraints = CA:false >> ca.conf
echo subjectKeyIdentifier = hash >> ca.conf
echo authorityKeyIdentifier = keyid:always >> ca.conf
echo keyUsage = digitalSignature,keyEncipherment >> ca.conf
echo extendedKeyUsage = serverAuth,clientAuth >> ca.conf

:: Variables used
SET COUNTRY=US
SET STATE=Arizona
SET LOCALITY=Chandler
SET ORGANIZATION=Ace Point Inc.
SET SERVER_FQDN=%ComputerName%.local
SET CA_KEY_PASSWORD=ca123key456
SET SERVER_KEY_PASSWORD=server123key456
SET SERVER_STORE_PASSWORD=server123store456

:: This file tracks the next available certficate serial number
IF EXIST serial (
    echo 0001 > serial
) ELSE (
    echo 0001 > serial
)

:: The CA records the certificates it signs here
echo. > certindex

:: Create CA certificate
openssl req ^
    -newkey rsa:4096 ^
    -keyout ca.key ^
    -nodes ^
    -x509 -days 365  -out ca.crt ^
    -subj "/C=%COUNTRY%/ST=%STATE%/L=%LOCALITY%/O=%ORGANIZATION%/CN=PlatformRootCA"

:: Create a CSR - certificate signing request
openssl req ^
    -newkey rsa:1024 ^
    -keyout server.key ^
    -nodes ^
    -out server.csr ^
    -subj "/C=%COUNTRY%/ST=%STATE%/L=%LOCALITY%/O=%ORGANIZATION%/CN=%SERVER_FQDN%"

:: Sign the CSR
:: openssl ca -batch -config ca.conf -notext -in server.csr -out server.crt
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt -days 365

:: Create a pkcs12 key store
openssl pkcs12 ^
    -inkey server.key -in server.crt ^
    -export -passout pass:%SERVER_STORE_PASSWORD% -out server.p12

:: Import pkcs12 store into java keystore format 
keytool -importkeystore ^
    -srckeystore server.p12     -srcstoretype PKCS12 -srcstorepass %SERVER_STORE_PASSWORD% ^
    -destkeystore keystore.p12 -deststoretype PKCS12 -deststorepass %SERVER_STORE_PASSWORD%

