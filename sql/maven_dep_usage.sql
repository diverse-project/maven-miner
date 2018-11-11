-- phpMyAdmin SQL Dump
-- version 4.8.3
-- https://www.phpmyadmin.net/
--
-- Host: serveur-du-placard.ml:53306:53306
-- Generation Time: Nov 09, 2018 at 10:15 AM
-- Server version: 10.2.10-MariaDB-10.2.10+maria~jessie
-- PHP Version: 7.2.8

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `maven_dep_usage`
--

-- --------------------------------------------------------

--
-- Table structure for table `api_member`
--

CREATE TABLE `api_member` (
  `id` int(11) NOT NULL,
  `package` text NOT NULL,
  `name` text NOT NULL,
  `libraryid` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `api_usage`
--

CREATE TABLE `api_usage` (
  `clientid` int(11) NOT NULL,
  `apimemberid` int(11) NOT NULL,
  `nb` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `client`
--

CREATE TABLE `client` (
  `id` int(11) NOT NULL,
  `coordinates` text NOT NULL,
  `groupid` text NOT NULL,
  `artifactid` text NOT NULL,
  `version` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `dependency`
--

CREATE TABLE `dependency` (
  `clientid` int(11) NOT NULL,
  `libraryid` int(11) NOT NULL,
  `intensity` int(11) DEFAULT NULL,
  `diversity` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `library`
--

CREATE TABLE `library` (
  `id` int(11) NOT NULL,
  `coordinates` text NOT NULL,
  `groupid` text NOT NULL,
  `artifactid` text NOT NULL,
  `version` text NOT NULL,
  `api_size` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `api_member`
--
ALTER TABLE `api_member`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_api_member_to_libraryid` (`libraryid`);

--
-- Indexes for table `api_usage`
--
ALTER TABLE `api_usage`
  ADD KEY `fk_api_usage_to_clientid` (`clientid`),
  ADD KEY `fk_api_usage_to_api_member` (`apimemberid`);

--
-- Indexes for table `client`
--
ALTER TABLE `client`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `coordinates` (`coordinates`(256));

--
-- Indexes for table `dependency`
--
ALTER TABLE `dependency`
  ADD KEY `fk_clientid` (`clientid`),
  ADD KEY `fk_libraryid` (`libraryid`);

--
-- Indexes for table `library`
--
ALTER TABLE `library`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `coordinates` (`coordinates`(256));

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `api_member`
--
ALTER TABLE `api_member`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `client`
--
ALTER TABLE `client`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `library`
--
ALTER TABLE `library`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `api_member`
--
ALTER TABLE `api_member`
  ADD CONSTRAINT `fk_api_member_to_libraryid` FOREIGN KEY (`libraryid`) REFERENCES `library` (`id`);

--
-- Constraints for table `api_usage`
--
ALTER TABLE `api_usage`
  ADD CONSTRAINT `fk_api_usage_to_api_member` FOREIGN KEY (`apimemberid`) REFERENCES `api_member` (`id`),
  ADD CONSTRAINT `fk_api_usage_to_clientid` FOREIGN KEY (`clientid`) REFERENCES `client` (`id`);

--
-- Constraints for table `dependency`
--
ALTER TABLE `dependency`
  ADD CONSTRAINT `fk_clientid` FOREIGN KEY (`clientid`) REFERENCES `client` (`id`),
  ADD CONSTRAINT `fk_libraryid` FOREIGN KEY (`libraryid`) REFERENCES `library` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
