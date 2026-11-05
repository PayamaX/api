create table predefined_origin (
	id bigint not null default nextval('id'),
	global_id uuid not null,
	origin varchar(255) not null,
	owner_global_id uuid,
	position integer,
	rank float(53) not null,
	primary key (id)
);

create table payamak_report (
	id bigint not null default nextval('id'),
	body varchar(255),
	decided_diagnostics varchar(255),
	desired_diagnostics varchar(255),
	global_id uuid,
	hash varchar(255),
	origin varchar(255),
	reporter_comment varchar(255),
	repeated_count integer not null,
	reporter uuid,
	primary key (id)
);

alter table if exists payamak_report
	add constraint uq__payamak_report___global_id unique (global_id);

alter table if exists predefined_origin
	add constraint uq__predefined_origin__global_id unique (global_id);