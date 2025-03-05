namespace PayamaX.Portal.Contexts;

using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;
using Microsoft.Extensions.Configuration;
using PayamaX.Portal.Contexts;
using System.IO;

/// <summary>
/// 
/// </summary>
public class ApplicationDbContextFactory : IDesignTimeDbContextFactory<ApplicationDbContext>
{
    /// <summary>
    /// 
    /// </summary>
    /// <param name="args"></param>
    /// <returns></returns>
    public ApplicationDbContext CreateDbContext(string[] args)
    {
        var optionsBuilder = new DbContextOptionsBuilder<ApplicationDbContext>();

        // Build the configuration to use the connection string from appsettings.json
        var configuration = new ConfigurationBuilder()
            .SetBasePath(Directory.GetCurrentDirectory())
            .AddJsonFile("appsettings.json")
            .Build();

        // Configure your DbContext to use SQL Server or your chosen provider
        optionsBuilder.UseNpgsql("User ID=px;Password=px;Host=localhost;Port=15432;Database=px;");

        return new ApplicationDbContext(optionsBuilder.Options);
    }
}
