-- Initialize enterprise_percussion database and tables
CREATE DATABASE IF NOT EXISTS enterprise_percussion;
USE enterprise_percussion;

-- Roles
CREATE TABLE IF NOT EXISTS role (
  role_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_name VARCHAR(50) NOT NULL UNIQUE
);

-- Users
CREATE TABLE IF NOT EXISTS `user` (
  user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(100) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  phone VARCHAR(20),
  address TEXT,
  enabled BOOLEAN DEFAULT TRUE,
  created_date DATETIME NOT NULL,
  updated_date DATETIME,
  role_id BIGINT NOT NULL,
  CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES role(role_id)
);

-- Departments
CREATE TABLE IF NOT EXISTS department (
  department_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  department_name VARCHAR(100) NOT NULL UNIQUE,
  manager_of_department VARCHAR(100)
);

-- Categories
CREATE TABLE IF NOT EXISTS category (
  category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  category_name VARCHAR(100) NOT NULL UNIQUE
);

-- Products
CREATE TABLE IF NOT EXISTS product (
  product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(150) NOT NULL,
  user_id BIGINT,
  price_per_product DECIMAL(10,2) NOT NULL,
  number_of_quantities INT NOT NULL,
  department_id BIGINT,
  category_id BIGINT,
  description TEXT,
  status ENUM('CLOSED','ACTIVE','PENDING_FOR_APPROVAL') NOT NULL DEFAULT 'ACTIVE',
  created_date DATETIME,
  update_date DATETIME,
  CONSTRAINT fk_product_user FOREIGN KEY (user_id) REFERENCES `user`(user_id),
  CONSTRAINT fk_product_department FOREIGN KEY (department_id) REFERENCES department(department_id),
  CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category(category_id)
);

-- Suppliers
CREATE TABLE IF NOT EXISTS supplier (
  supplier_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT,
  name VARCHAR(150) NOT NULL,
  phone VARCHAR(20),
  address TEXT,
  email VARCHAR(150),
  gst_number VARCHAR(50),
  status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  rating DECIMAL(2,1),
  feedback TEXT,
  CONSTRAINT fk_supplier_product FOREIGN KEY (product_id) REFERENCES product(product_id)
);

-- Purchase requests
CREATE TABLE IF NOT EXISTS purchase_request (
  request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  product_id BIGINT,
  department_id BIGINT,
  quantity INT NOT NULL,
  total_price DECIMAL(10,2) NOT NULL,
  status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
  created_date DATETIME,
  update_date DATETIME,
  CONSTRAINT fk_pr_user FOREIGN KEY (user_id) REFERENCES `user`(user_id),
  CONSTRAINT fk_pr_product FOREIGN KEY (product_id) REFERENCES product(product_id),
  CONSTRAINT fk_pr_department FOREIGN KEY (department_id) REFERENCES department(department_id)
);

-- Approval hierarchy (optional)
CREATE TABLE IF NOT EXISTS approval_hierarchy (
  hierarchy_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  approver_role_id BIGINT NOT NULL,
  min_amount DECIMAL(10,2) NOT NULL,
  max_amount DECIMAL(10,2),
  sequence_order INT NOT NULL,
  status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_approval_hierarchy_role FOREIGN KEY (approver_role_id) REFERENCES role(role_id)
);

-- Seed default roles (if not using DataInitializer)
INSERT INTO role (role_name)
SELECT * FROM (SELECT 'ADMIN') AS tmp
WHERE NOT EXISTS (SELECT role_name FROM role WHERE role_name = 'ADMIN') LIMIT 1;

INSERT INTO role (role_name)
SELECT * FROM (SELECT 'USER') AS tmp
WHERE NOT EXISTS (SELECT role_name FROM role WHERE role_name = 'USER') LIMIT 1;

-- Note: created_date fields are left for application to set via entity lifecycle
