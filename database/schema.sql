/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/SQLTemplate.sql to edit this template
 */
/**
 * Author:  mgmmo
 * Created: 31 ene 2026
 */

-- ============================================
-- SISTEMA ANDYNAILS - ESTRUCTURA DE BASE DE DATOS
-- Archivo: schema.sql
-- Solo estructura, sin datos de prueba
-- ============================================

-- Crear base de datos si no existe
CREATE DATABASE IF NOT EXISTS andynails;
USE andynails;

-- ============================================
-- TABLA: TIPO_USUARIO (Roles del sistema)
-- ============================================
CREATE TABLE tipo_usuario (
    idTipo_Usuario INT UNSIGNED NOT NULL AUTO_INCREMENT,
    Nombre VARCHAR(20),
    PRIMARY KEY (idTipo_Usuario)
);

-- ============================================
-- TABLA: USUARIOS (Clientes y empleados)
-- ============================================
CREATE TABLE usuarios (
    idUsuarios INT UNSIGNED NOT NULL AUTO_INCREMENT,
    Tipo_Usuario_idTipo_Usuario INT UNSIGNED NOT NULL,
    Nombre VARCHAR(10),
    Materno VARCHAR(10),
    Paterno VARCHAR(10),
    Telefono VARCHAR(10),
    Correo VARCHAR(50) UNIQUE,
    Contraseña VARCHAR(100),
    fecha_registro DATE DEFAULT CURDATE(),
    PRIMARY KEY (idUsuarios),
    FOREIGN KEY (Tipo_Usuario_idTipo_Usuario) 
        REFERENCES tipo_usuario(idTipo_Usuario)
        ON DELETE CASCADE
);

-- ============================================
-- TABLA: BLOQUEO_HORARIO (Horarios no disponibles)
-- ============================================
CREATE TABLE bloqueo_horario (
    idBloqueo_Horario INT UNSIGNED NOT NULL AUTO_INCREMENT,
    idUsuarios INT UNSIGNED NOT NULL,
    Fecha DATE,
    Hora_inicio TIME,
    Hora_fin TIME,
    Motivo VARCHAR(30),
    PRIMARY KEY (idBloqueo_Horario),
    FOREIGN KEY (idUsuarios) 
        REFERENCES usuarios(idUsuarios)
        ON DELETE CASCADE
);

-- ============================================
-- TABLA: SERVICIOS (Catálogo del salón)
-- ============================================
CREATE TABLE servicios (
    idServicios INT UNSIGNED NOT NULL AUTO_INCREMENT,
    Nombre_servicio VARCHAR(50),
    Descripcion TEXT,
    Precio DECIMAL(10,2),
    PRIMARY KEY (idServicios)
);

-- ============================================
-- TABLA: CATEGORIA_SERVICIO (Categorías)
-- ============================================
CREATE TABLE categoria_servicio (
    idCategoria_Servicio INT UNSIGNED NOT NULL AUTO_INCREMENT,
    idServicios INT UNSIGNED NOT NULL,
    Imagen_Archivo VARCHAR(255),
    Nombre_categoria VARCHAR(100),
    Descripcion TEXT,
    Precio DECIMAL(10,2),
    PRIMARY KEY (idCategoria_Servicio),
    FOREIGN KEY (idServicios) 
        REFERENCES servicios(idServicios)
        ON DELETE CASCADE
);

-- ============================================
-- TABLA: METODO_PAGO (Formas de pago)
-- ============================================
CREATE TABLE metodo_pago (
    idMetodo_Pago INT UNSIGNED NOT NULL AUTO_INCREMENT,
    Metodo_pago VARCHAR(20),
    PRIMARY KEY (idMetodo_Pago)
);

-- ============================================
-- TABLA: PAGO (Registro de transacciones)
-- ============================================
CREATE TABLE pago (
    idPago INT UNSIGNED NOT NULL AUTO_INCREMENT,
    idMetodo_Pago INT UNSIGNED,
    Monto_pagado DECIMAL(10,2),
    fecha_pago DATE,
    Estado_pago VARCHAR(50),
    Comprobante VARCHAR(255),
    Clave INT UNSIGNED,
    Monto DOUBLE,
    Concepto VARCHAR(50),
    Banco VARCHAR(50),
    idUsuarios INT UNSIGNED,
    PRIMARY KEY (idPago),
    FOREIGN KEY (idMetodo_Pago) 
        REFERENCES metodo_pago(idMetodo_Pago)
        ON DELETE SET NULL,
    FOREIGN KEY (idUsuarios) 
        REFERENCES usuarios(idUsuarios)
        ON DELETE SET NULL
);

-- ============================================
-- TABLA: CITA (Agenda de citas)
-- ============================================
CREATE TABLE cita (
    idCita INT UNSIGNED NOT NULL AUTO_INCREMENT,
    idUsuarios INT UNSIGNED NOT NULL,
    Fecha DATE,
    Hora TIME,
    Estado VARCHAR(20),
    Pago_idPago INT UNSIGNED,
    PRIMARY KEY (idCita),
    FOREIGN KEY (idUsuarios) 
        REFERENCES usuarios(idUsuarios)
        ON DELETE CASCADE,
    FOREIGN KEY (Pago_idPago) 
        REFERENCES pago(idPago)
        ON DELETE SET NULL
);

-- ============================================
-- TABLA: CITA_HAS_SERVICIOS (Servicios por cita)
-- ============================================
CREATE TABLE cita_has_servicios (
    idCita INT UNSIGNED NOT NULL,
    idServicios INT UNSIGNED NOT NULL,
    Pago_idPago INT UNSIGNED NOT NULL,
    Monto_anticipo DECIMAL(10,2),
    PRIMARY KEY (idCita, idServicios),
    FOREIGN KEY (idCita) 
        REFERENCES cita(idCita)
        ON DELETE CASCADE,
    FOREIGN KEY (idServicios) 
        REFERENCES servicios(idServicios)
        ON DELETE CASCADE,
    FOREIGN KEY (Pago_idPago) 
        REFERENCES pago(idPago)
        ON DELETE CASCADE
);

-- ============================================
-- MENSAJE FINAL
-- ============================================
SELECT 'Base de datos Andynails creada exitosamente' AS Mensaje;
SHOW TABLES;