create table company(
	id integer not null,
	name character varying,
	constraint company_pkey primary key (id)
);
create table person(
	id integer not null,
	name character varying,
	company_id integer references company(id),
	constraint person_pkey primary key (id)
);
insert into company (id, name) values (1, 'Nike');
insert into company (id, name) values (2, 'Coca-Cola');
insert into company (id, name) values (3, 'Apple');
insert into company (id, name) values (4, 'Google');
insert into company (id, name) values (5, 'Microsoft');
insert into person (id, name, company_id) values (1, 'Nikolay', 2);
insert into person (id, name, company_id) values (2, 'Vasiliy', 1);
insert into person (id, name, company_id) values (3, 'Vladimir', 2);
insert into person (id, name, company_id) values (4, 'Karina', 3);
insert into person (id, name, company_id) values (5, 'Boris', 5);
insert into person (id, name, company_id) values (6, 'Olga', 4);
insert into person (id, name, company_id) values (7, 'Oleg', 2);
insert into person (id, name, company_id) values (8, 'Evgeniy', 5);
insert into person (id, name, company_id) values (9, 'Anatoliy', 4);
insert into person (id, name, company_id) values (0, 'Marina', 1);
--1
select company.name as company, person.name as person
from company join person on company.id = company_id
where company_id != 5
order by company.name;
--2
--command 1
create view buff as
select company.name, count(person.name) as c
from company join person on company.id = company_id
group by company.name;
--command 2
select buff.name, buff.c
from buff
where buff.c = (
	select max(buff.c)
	from buff
);