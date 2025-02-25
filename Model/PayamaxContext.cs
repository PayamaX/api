using Microsoft.EntityFrameworkCore;

namespace api.Model;

public class PayamaxContext(DbContextOptions<PayamaxContext> options) : DbContext(options)
{
    public required DbSet<PayamakExpectedProcessResultEntity> Payamaks { get; set; }
    public required DbSet<PayamakRuleProcessResultEntity> ProcessResults { get; set; }
}