using FluentNHibernate.Automapping;
using FluentNHibernate.Cfg;
using FluentNHibernate.Cfg.Db;
using NHibernate.Tool.hbm2ddl;
using No1.Portal.Configs;
using PayamaX.Portal.Config.NHibernateFluentAutoMap;
using PayamaX.Portal.Contracts;
using PayamaX.Portal.Model;
using PayamaX.Portal.Services;

namespace PayamaX.Portal;

public class Program
{
    public static void Main(string[] args)
    {
        var builder = WebApplication.CreateBuilder(args);
        builder.Services.AddScoped<IPayamaksContract, PayamaksService>();
        builder.Services.AddEndpointsApiExplorer();
        builder.Services.AddSwaggerGen();
        builder.Services.Configure<ConnectionString>(builder.Configuration.GetSection("ConnectionString"));
        var cs =
            (builder.Configuration.GetSection("ConnectionString") ??
             throw new Exception("ConnectionString section can not be found")).Get<ConnectionString>() ??
            throw new Exception("ConnectionString can't be converted to a connection string");
        builder.Services.AddControllers();

        var payamaxNhFluentAutoConfig = new PayamaxConfig();
        var autoPersistenceModel = AutoMap.AssemblyOf<Program>(payamaxNhFluentAutoConfig);
        autoPersistenceModel.Conventions.Add(new PayamaxConventions());
        autoPersistenceModel.Conventions.Add(new StringLengthConvention());
        var sessionFactory = Fluently.Configure()
            .Database(() => PostgreSQLConfiguration.Standard.ConnectionString(
                $"User ID={cs.Username};Password={cs.Password};Host={cs.Host};Port={cs.Port};Database={cs.Database};"))
            .Mappings(m => m.AutoMappings.Add(autoPersistenceModel))
            .ExposeConfiguration(cfg => new SchemaUpdate(cfg).Execute(true, true))
            .BuildSessionFactory();
        builder.Services.AddScoped<NHibernate.ISession>(_ => sessionFactory.OpenSession());
        builder.Services.AddScoped<PayamaxRepo>();

        var app = builder.Build();

        app.UseHttpsRedirection();
        app.MapControllers();
        app.UseSwagger();
        app.UseSwaggerUI();

        app.Run();
    }
}