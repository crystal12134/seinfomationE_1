CREATE TABLE IF NOT EXISTS seinfo_security_meta(meta_key VARCHAR(64) PRIMARY KEY,meta_value VARCHAR(128));
MERGE INTO seinfo_security_meta KEY(meta_key) VALUES ('schema_version','2'),('data_key_id','66687aadf862bd776c8fc18b8e9f8e20089714856ee233b3902a591d0d5f2925'),('lookup_key_id','72cd6e8422c407fb6d098690f1130b7ded7ec2f7f5e1d30bd9d521f015363793');
CREATE TABLE IF NOT EXISTS seinfo_password_reset(reset_key CHAR(64) PRIMARY KEY,user_uid INT UNIQUE,password_version CHAR(64),created_at TIMESTAMP,expires_at TIMESTAMP);
