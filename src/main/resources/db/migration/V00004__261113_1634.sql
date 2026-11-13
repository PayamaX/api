alter table if exists payamak_report add column detected_diagnostics varchar (255) check ((detected_diagnostics in ('Important','Useful','Spam')));
alter table if exists payamak_report add column expected_diagnostics varchar (255) check ((expected_diagnostics in ('Important','Useful','Spam')));
alter table if exists payamak_report add column flagged boolean not null;
alter table if exists payamak_report add column reporter_user_global_id uuid;
alter table if exists payamak_report add column reviewer_comment varchar (255);
alter table if exists payamak_report add column sha512 varchar (255);
alter table if exists payamak_report add column state varchar (255) check ((state in ('PENDING','REVIEWED','POSTPONED')));
create table payamak_rule_process_result_report
(
	id                         bigint  not null default nextval('id'),
	general_rank               float4  not null,
	global_id                  uuid,
	position                   integer not null,
	processor_name             varchar(255),
	processor_provided_comment varchar(255),
	processor_version          varchar(255),
	user_override_rank         float4,
	report_id                  bigint,
	primary key (id)
);
create index ix__payamak_report__state on payamak_report (state);
create index ix__payamak_report__origin on payamak_report (origin);
create index ix__payamak_report__flagged on payamak_report (flagged);
alter table if exists payamak_report drop constraint if exists uq__payamak_report__global_id;
alter table if exists payamak_report add constraint uq__payamak_report__global_id unique (global_id);
alter table if exists payamak_rule_process_result_report drop constraint if exists uq__payamak_rule_process_result_report__global_id;
alter table if exists payamak_rule_process_result_report add constraint uq__payamak_rule_process_result_report__global_id unique (global_id);
alter table if exists ranked_origin drop constraint if exists uq__ranked_origin__origin;
alter table if exists ranked_origin add constraint uq__ranked_origin__origin unique (origin);
alter table if exists payamak_rule_process_result_report add constraint fk__payamak_rule_process_result_report__report_id foreign key (report_id) references payamak_report;
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
