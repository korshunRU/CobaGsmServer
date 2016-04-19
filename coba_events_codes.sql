-- phpMyAdmin SQL Dump
-- version 4.2.5
-- http://www.phpmyadmin.net
--
-- Хост: localhost
-- Время создания: Апр 19 2016 г., 11:23
-- Версия сервера: 5.5.25
-- Версия PHP: 5.3.13

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- База данных: `coba_web_room`
--

-- --------------------------------------------------------

--
-- Структура таблицы `coba_events_codes`
--

CREATE TABLE IF NOT EXISTS `coba_events_codes` (
`id` tinyint(2) NOT NULL,
  `code` varchar(2) NOT NULL,
  `desc` varchar(255) NOT NULL
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=33 ;

--
-- Дамп данных таблицы `coba_events_codes`
--

INSERT INTO `coba_events_codes` (`id`, `code`, `desc`) VALUES
(1, 'A0', 'Постановка (16й пользователь)'),
(2, 'A1', 'Постановка (1й пользователь)'),
(3, 'A2', 'Постановка (2й пользователь)'),
(4, 'A3', 'Постановка (3й пользователь)'),
(5, 'A4', 'Постановка (4й пользователь)'),
(6, 'A5', 'Постановка (5й пользователь)'),
(7, 'A6', 'Постановка (6й пользователь)'),
(8, 'A7', 'Постановка (7й пользователь)'),
(9, 'A8', 'Постановка (8й пользователь)'),
(10, 'A9', 'Постановка (9й пользователь)'),
(11, 'AA', 'Постановка (10й пользователь)'),
(12, 'AB', 'Постановка (11й пользователь)'),
(13, 'AC', 'Постановка (12й пользователь)'),
(14, 'AD', 'Постановка (13й пользователь)'),
(15, 'AE', 'Постановка (14й пользователь)'),
(16, 'AF', 'Постановка (15й пользователь)'),
(17, 'D0', 'Снятие (16й пользователь)'),
(18, 'D1', 'Снятие (1й пользователь)'),
(19, 'D2', 'Снятие (2й пользователь)'),
(20, 'D3', 'Снятие (3й пользователь)'),
(21, 'D4', 'Снятие (4й пользователь)'),
(22, 'D5', 'Снятие (5й пользователь)'),
(23, 'D6', 'Снятие (6й пользователь)'),
(24, 'D7', 'Снятие (7й пользователь)'),
(25, 'D8', 'Снятие (8й пользователь)'),
(26, 'D9', 'Снятие (9й пользователь)'),
(27, 'DA', 'Снятие (10й пользователь)'),
(28, 'DB', 'Снятие (11й пользователь)'),
(29, 'DC', 'Снятие (12й пользователь)'),
(30, 'DD', 'Снятие (13й пользователь)'),
(31, 'DE', 'Снятие (14й пользователь)'),
(32, 'DF', 'Снятие (15й пользователь)');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `coba_events_codes`
--
ALTER TABLE `coba_events_codes`
 ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `coba_events_codes`
--
ALTER TABLE `coba_events_codes`
MODIFY `id` tinyint(2) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=33;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
