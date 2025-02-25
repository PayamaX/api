using api.Model;
using Microsoft.EntityFrameworkCore;
using No1.Portal.Configs;

namespace api;

public class Program
{

    class A
    {
        
    }
    public static void Main(string[] args)
    {
        var builder = WebApplication.CreateBuilder(args);
        builder.Services.AddEndpointsApiExplorer();
        builder.Services.AddSwaggerGen();
        builder.Services.Configure<ConnectionString>(builder.Configuration.GetSection("ConnectionString"));
        var connectionString = (builder.Configuration.GetSection("ConnectionString")?? throw new Exception("ConnectionString section can not be found")).Get<ConnectionString>() ?? throw new Exception("ConnectionString can't be converted to a connection string");
        builder.Services.AddDbContext<PayamaxContext>(options =>
            options.UseNpgsql($"Server={connectionString.Host}:{connectionString.Port};Database={connectionString.Database};UID={connectionString.Username};PWD={connectionString.Password}"));

        builder.Services.AddControllers();
        var app = builder.Build();

        app.UseHttpsRedirection();
        app.MapControllers();
        app.UseSwagger();
        app.UseSwaggerUI();

        app.Run();
    }
}