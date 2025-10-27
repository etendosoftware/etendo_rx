-- Insert test configurations for Etendo RX
-- This script sets up the necessary configuration data for running tests

-- Insert SMFSWS configuration
INSERT INTO smfsws_config (
    smfsws_config_id, ad_client_id, ad_org_id, isactive, created, createdby,
    updated, updatedby, expirationtime, privatekey
) VALUES (
    '07054A65ACCB423F90D270130DCD02E4',
    '0',
    '0',
    'Y',
    '2025-04-16 13:19:11.492',
    '100',
    '2025-04-16 13:19:11.492',
    '100',
    0,
    '{"private-key":"-----BEGIN PRIVATE KEY-----MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgoP6uMq5AuRjREe3oUUeuh6LJTYWTnPvr7Ds8+mstk5+hRANCAASjRJgZeEBfLflXzTYeSFuPSlwBGlVKXDY1+baWJM2L0E+o3NLyLWFY1qjfudRUY8H3AkSoNY3KmfT67h7We56F-----END PRIVATE KEY-----","public-key":"-----BEGIN PUBLIC KEY-----MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEo0SYGXhAXy35V802Hkhbj0pcARpVSlw2Nfm2liTNi9BPqNzS8i1hWNao37nUVGPB9wJEqDWNypn0+u4e1nuehQ==-----END PUBLIC KEY-----"}'
);

-- Insert Etendo RX service configurations
INSERT INTO etrx_config (
    etrx_config_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby,
    service_name, service_url, updateable_configs, public_url, restart_services
) VALUES
('B9393395A3074D5FBD3E1863D9BD2671', '0', '0', 'Y', '2025-04-16 13:27:01.655', '100', '2025-04-16 13:27:01.655', '100', 'config', 'http://localhost:8888', 'N', 'http://localhost:8888', 'Y'),
('E00B0AADFF024417BF67D0313AAFCF28', '0', '0', 'Y', '2025-04-16 13:27:01.66',  '100', '2025-04-16 13:27:01.66',  '100', 'auth', 'http://localhost:8094', 'Y', 'http://localhost:8094', 'Y'),
('1E738098F45344DAAD18A8FC352E0876', '0', '0', 'Y', '2025-04-16 13:27:01.669', '100', '2025-04-16 13:27:01.669', '100', 'das', 'http://localhost:8092', 'Y', 'http://localhost:8092', 'Y'),
('80B5E214E3B444FFBF48FA7F563E8F0D', '0', '0', 'Y', '2025-04-16 13:27:01.673', '100', '2025-04-16 13:27:01.673', '100', 'edge', 'http://localhost:8096', 'Y', 'http://localhost:8096', 'Y'),
('B3FB8056DC2F4D37BD0B85CD0CB998A8', '0', '0', 'Y', '2025-04-16 13:27:01.676', '100', '2025-04-16 13:27:01.676', '100', 'asyncprocess', 'http://localhost:9092', 'Y', 'http://localhost:9092', 'Y'),
('0976C13F5C364F918D9A94282A0EF980', '0', '0', 'Y', '2025-04-16 13:27:01.689', '100', '2025-04-16 13:27:01.689', '100', 'obconnsrv', 'http://localhost:8101', 'Y', 'http://localhost:8101', 'Y'),
('C1EB36E4B2A1442FB45C78926CDD4E36', '0', '0', 'Y', '2025-04-16 13:27:01.691', '100', '2025-04-16 13:27:01.691', '100', 'worker', 'http://localhost:0', 'Y', 'http://localhost:0', 'Y');

-- Insert service parameters
INSERT INTO etrx_service_param (
    etrx_service_param_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby,
    param_value, param_key, etrx_config_id
) VALUES
('5C321A9820CE4AA496F07D06F24CC70E', '0', '0', 'Y', '2025-04-16 13:27:01.656', '100', '2025-04-16 13:27:01.656', '100', 'http://localhost:8092', 'das.url', 'B9393395A3074D5FBD3E1863D9BD2671'),
('FDBDF078070047E0A49D09FBDB87FDD6', '0', '0', 'Y', '2025-04-16 13:27:01.657', '100', '2025-04-16 13:27:01.657', '100', 'http://localhost:8080', 'classic.url', 'B9393395A3074D5FBD3E1863D9BD2671'),
('11950F994DBB40019E900209B0B0BDAE', '0', '0', 'Y', '2025-04-16 13:27:01.665', '100', '2025-04-16 13:27:01.665', '100', 'http://localhost:8092', 'das.url', 'E00B0AADFF024417BF67D0313AAFCF28'),
('37CF36206FC54E1996D28DEF4FAB7FC4', '0', '0', 'Y', '2025-04-16 13:27:01.666', '100', '2025-04-16 13:27:01.666', '100', 'http://localhost:8080', 'classic.url', 'E00B0AADFF024417BF67D0313AAFCF28'),
('5DC4588085AF42A38547A3E996988E62', '0', '0', 'Y', '2025-04-16 13:27:01.669', '100', '2025-04-16 13:27:01.669', '100', 'http://localhost:8092', 'das.url', '1E738098F45344DAAD18A8FC352E0876'),
('B107590E820F48328EA7EE7880DE5E65', '0', '0', 'Y', '2025-04-16 13:27:01.671', '100', '2025-04-16 13:27:01.671', '100', 'http://localhost:8080', 'classic.url', '1E738098F45344DAAD18A8FC352E0876'),
('F2F1FAD5AD284162A8BF70E538810D3D', '0', '0', 'Y', '2025-04-16 13:27:01.674', '100', '2025-04-16 13:27:01.674', '100', 'http://localhost:8092', 'das.url', '80B5E214E3B444FFBF48FA7F563E8F0D'),
('F14FE88FE871496D957FA5D299EF55F9', '0', '0', 'Y', '2025-04-16 13:27:01.675', '100', '2025-04-16 13:27:01.675', '100', 'http://localhost:8080', 'classic.url', '80B5E214E3B444FFBF48FA7F563E8F0D'),
('93A49116DBE449FDAF8F178058215223', '0', '0', 'Y', '2025-04-16 13:27:01.677', '100', '2025-04-16 13:27:01.677', '100', 'http://localhost:8092', 'das.url', 'B3FB8056DC2F4D37BD0B85CD0CB998A8'),
('52D7DAA3B85E466F98D112B2B68FBB72', '0', '0', 'Y', '2025-04-16 13:27:01.688', '100', '2025-04-16 13:27:01.688', '100', 'http://localhost:8080', 'classic.url', 'B3FB8056DC2F4D37BD0B85CD0CB998A8'),
('4F679F105FFB40429EE0AEA007E35F30', '0', '0', 'Y', '2025-04-16 13:27:01.69',  '100', '2025-04-16 13:27:01.69',  '100', 'http://localhost:8092', 'das.url', '0976C13F5C364F918D9A94282A0EF980'),
('7DF211A4800F4BEDABD264A906736EC3', '0', '0', 'Y', '2025-04-16 13:27:01.69',  '100', '2025-04-16 13:27:01.69',  '100', 'http://localhost:8080', 'classic.url', '0976C13F5C364F918D9A94282A0EF980'),
('18F282F7C6D64DE69D2BB96E039C8798', '0', '0', 'Y', '2025-04-16 13:27:01.692', '100', '2025-04-16 13:27:01.692', '100', 'http://localhost:8092', 'das.url', 'C1EB36E4B2A1442FB45C78926CDD4E36'),
('90CCE704383048779AD4DCE4CF4C4A80', '0', '0', 'Y', '2025-04-16 13:27:01.692', '100', '2025-04-16 13:27:01.692', '100', 'http://localhost:8080', 'classic.url', 'C1EB36E4B2A1442FB45C78926CDD4E36');
