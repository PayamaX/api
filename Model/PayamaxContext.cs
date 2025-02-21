using Microsoft.EntityFrameworkCore;

namespace api.Model;

public class PayamaxContext : DbContext
{
    public required DbSet<PayamakExpectedProcessResultEntity> Payamaks { get; set; }
    public required DbSet<PayamakRuleProcessResultEntity> ProcessResults { get; set; }

    protected override void OnConfiguring(DbContextOptionsBuilder optionsBuilder) => optionsBuilder.UseNpgsql(@"Host");
}