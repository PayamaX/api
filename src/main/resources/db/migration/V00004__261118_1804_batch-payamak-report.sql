alter table payamak_report
	add column state varchar(255) not null check ((state in ('PENDING', 'REVIEWED', 'POSTPONED')));
alter table payamak_report
	alter column origin set data type varchar(16);
alter table payamak_report
	alter column body set data type varchar(4096);
alter table payamak_report
	add column sha512 varchar(88);
alter table payamak_report
	add column reporter_user_global_id uuid;
alter table payamak_report
	alter column reporter_comment set data type varchar(4096);
alter table payamak_report
	add column reviewer_comment varchar(4096);
alter table payamak_report
	add column flagged boolean not null;
alter table payamak_report
	add column detected_diagnostics varchar(255) not null check ((detected_diagnostics in ('Important', 'Useful', 'Spam')));
alter table payamak_report
	add column expected_diagnostics varchar(255) check ((expected_diagnostics in ('Important', 'Useful', 'Spam')));
create table payamak_report_rule_process_result
(
	id                         bigint default nextval('id') not null,
	global_id                  uuid                         not null,
	payamak_report_id          bigint,
	processor_name             varchar(32)                  not null,
	processor_version          integer                      not null,
	general_rank               float4                       not null,
	user_override_rank         float4,
	processor_provided_comment varchar(4096),
	position                   integer                      not null,
	primary key (id)
);
alter table ranked_origin
	alter column origin set data type varchar(16);
alter table ranked_origin
	alter column comment set data type varchar(512);
create index ix__payamak_report__state on payamak_report (state);
create index ix__payamak_report__origin on payamak_report (origin);
create index ix__payamak_report__flagged on payamak_report (flagged);
alter table payamak_report drop constraint uq__payamak_report__global_id;
alter table payamak_report
	add constraint uq__payamak_report__global_id unique (global_id);
create index ix__payamak_report_rule_process_result__payamak_report_id on payamak_report_rule_process_result (payamak_report_id);
alter table payamak_report_rule_process_result drop constraint uq__payamak_report_rule_process_result__global_id;
alter table payamak_report_rule_process_result
	add constraint uq__payamak_report_rule_process_result__global_id unique (global_id);
alter table ranked_origin drop constraint uq__ranked_origin__origin;
alter table ranked_origin
	add constraint uq__ranked_origin__origin unique (origin);
alter table payamak_report_rule_process_result
	add constraint fk__payamak_report_rule_process_result__payamak_report_id foreign key (payamak_report_id) references payamak_report;
do
$$
begin
	if
exists (select 1 from "public"."payamak_report" where "desired_diagnostics" is not null) then
		raise exception 'column payamak_report.desired_diagnostics has a non-null value';
end if;
alter table "public"."payamak_report" drop column "desired_diagnostics";
end
$$;
do
$$
begin
	if
exists (select 1 from "public"."payamak_report" where "hash" is not null) then
		raise exception 'column payamak_report.hash has a non-null value';
end if;
alter table "public"."payamak_report" drop column "hash";
end
$$;
do
$$
begin
	if
exists (select 1 from "public"."payamak_report" where "repeated_count" is not null) then
		raise exception 'column payamak_report.repeated_count has a non-null value';
end if;
alter table "public"."payamak_report" drop column "repeated_count";
end
$$;
do
$$
begin
	if
exists (select 1 from "public"."payamak_report" where "reporter" is not null) then
		raise exception 'column payamak_report.reporter has a non-null value';
end if;
alter table "public"."payamak_report" drop column "reporter";
end
$$;
do
$$
begin
	if
exists (select 1 from "public"."payamak_report" where "global_id" is null) then
		raise exception 'column payamak_report.global_id has a null value';
end if;
alter table "public"."payamak_report"
	alter column "global_id" set not null;
end
$$;
do
$$
begin
	if
exists (select 1 from "public"."payamak_report" where "origin" is null) then
		raise exception 'column payamak_report.origin has a null value';
end if;
alter table "public"."payamak_report"
	alter column "origin" set not null;
end
$$;
