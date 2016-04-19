-- phpMyAdmin SQL Dump
-- version 4.2.5
-- http://www.phpmyadmin.net
--
-- Хост: localhost
-- Время создания: Апр 19 2016 г., 11:09
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
-- Структура таблицы `coba_events_gsm`
--

CREATE TABLE IF NOT EXISTS `coba_events_gsm` (
`id` int(10) NOT NULL,
  `time` datetime NOT NULL,
  `object_id` mediumint(7) DEFAULT NULL,
  `code_id` mediumint(5) DEFAULT NULL,
  `event_id` tinyint(2) DEFAULT NULL
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=61 ;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `coba_events_gsm`
--
ALTER TABLE `coba_events_gsm`
 ADD PRIMARY KEY (`id`), ADD UNIQUE KEY `time` (`time`,`object_id`,`code_id`,`event_id`), ADD KEY `code_id` (`event_id`), ADD KEY `object_id` (`object_id`), ADD KEY `code_id_2` (`code_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `coba_events_gsm`
--
ALTER TABLE `coba_events_gsm`
MODIFY `id` int(10) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=61;
--
-- Ограничения внешнего ключа сохраненных таблиц
--

--
-- Ограничения внешнего ключа таблицы `coba_events_gsm`
--
ALTER TABLE `coba_events_gsm`
ADD CONSTRAINT `coba_events_gsm_ibfk_4` FOREIGN KEY (`code_id`) REFERENCES `coba_numbers_hex` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
ADD CONSTRAINT `coba_events_gsm_ibfk_1` FOREIGN KEY (`object_id`) REFERENCES `coba_objects` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
ADD CONSTRAINT `coba_events_gsm_ibfk_3` FOREIGN KEY (`event_id`) REFERENCES `coba_events_codes` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
