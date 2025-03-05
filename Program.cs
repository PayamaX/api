using FluentNHibernate.Automapping;
using FluentNHibernate.Cfg;
using FluentNHibernate.Cfg.Db;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.OpenApi.Models;
using NHibernate.Tool.hbm2ddl;
using No1.Portal.Configs;
using PayamaX.Portal.Config;
using PayamaX.Portal.Config.NHibernateFluentAutoMap;
using PayamaX.Portal.Config.Swagger;
using PayamaX.Portal.Contexts;
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
        var csObj =
            (builder.Configuration.GetSection("ConnectionString") ??
             throw new Exception("ConnectionString section can not be found")).Get<ConnectionString>() ??
            throw new Exception("ConnectionString can't be converted to a connection string");
        var csStr = $"User ID={csObj.Username};Password={csObj.Password};Host={csObj.Host};Port={csObj.Port};Database={csObj.Database};";
        builder.Services.AddControllers();

        var payamaxNhFluentAutoConfig = new PayamaxConfig();
        var autoPersistenceModel = AutoMap.AssemblyOf<Program>(payamaxNhFluentAutoConfig);
        autoPersistenceModel.Conventions.Add(new PayamaxConventions());
        autoPersistenceModel.Conventions.Add(new StringLengthConvention());
        var sessionFactory = Fluently.Configure()
            .Database(() => PostgreSQLConfiguration.Standard.ConnectionString(csStr))
            .Mappings(m => m.AutoMappings.Add(autoPersistenceModel))
            .ExposeConfiguration(cfg => new SchemaUpdate(cfg).Execute(true, true))
            .BuildSessionFactory();
        builder.Services.AddScoped<NHibernate.ISession>(_ => sessionFactory.OpenSession());
        builder.Services.AddScoped<PayamaxRepo>();
        builder.Services.AddSwaggerGen(c =>
        {
            c.AddSecurityDefinition("bearerAuth", new OpenApiSecurityScheme
            {
                Type = SecuritySchemeType.Http,
                Scheme = "bearer",
                BearerFormat = "JWT",
                Description = "JWT Authorization header using the Bearer scheme."
            });

            c.DocumentFilter<IdentityDocumentFilter>();
            c.OperationFilter<SecurityRequirementsOperationFilter>();
            c.IncludeXmlComments(Path.Combine(AppContext.BaseDirectory, "PayamaX.Portal.xml"));
            c.SwaggerDoc("identity", new OpenApiInfo { Title = "PayamaX Identity API", Version = "identity" });
            c.SwaggerDoc("public", new OpenApiInfo { Title = "PayamaX Public API", Version = "public" });
            c.SwaggerDoc("manager", new OpenApiInfo { Title = "PayamaX Manager API", Version = "manager" });
        });
        builder.Services.AddDbContext<ApplicationDbContext>(options => options.UseNpgsql(csStr));
        builder.Services.AddAuthorization();
        builder.Services.AddIdentityApiEndpoints<IdentityUser>().AddEntityFrameworkStores<ApplicationDbContext>();

        var app = builder.Build();

        app.MapGroup("/identity").MapIdentityApi<IdentityUser>();
        app.UseHttpsRedirection();
        app.MapControllers();
        //app.MapSwagger().RequireAuthorization();
        app.UseSwagger();
        app.UseSwaggerUI(c =>
        {
            c.SwaggerEndpoint("identity/swagger.json", "PayamaX Identity API");
            c.SwaggerEndpoint("public/swagger.json", "PayamaX Public API");
            c.SwaggerEndpoint("manager/swagger.json", "PayamaX Manager API");
        });

        app.Run();
    }
}