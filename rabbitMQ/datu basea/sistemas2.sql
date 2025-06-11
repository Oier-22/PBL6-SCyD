-- Crear la base de datos
CREATE DATABASE IF NOT EXISTS sistema_riego2;

-- Usar la base de datos
USE sistema_riego2;

CREATE TABLE IF NOT EXISTS Usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100),
    email VARCHAR(100) UNIQUE,
    contrasena VARCHAR(100)
);

-- Crear la tabla Parcela
CREATE TABLE IF NOT EXISTS Parcela (
    id INT(50) PRIMARY KEY,
    usuario_id INT NOT NULL,
    temperatura DOUBLE,
    humedad DOUBLE,
    viento DOUBLE,
    radiacion DOUBLE,
    precipitacion DOUBLE,
    tipoDePlanta VARCHAR(100),
    etapaCrecimiento VARCHAR(100),
    humedadSuelo DOUBLE,
    diaDelAnio INT,
    consumoAgua DOUBLE DEFAULT 0,
    FOREIGN KEY (usuario_id) REFERENCES Usuario(id) ON DELETE CASCADE
);

SELECT id FROM parcela where id=156 ORDER BY id DESC;

select * from parcela order by id asc;