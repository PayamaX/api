using FluentNHibernate.Automapping;

namespace PayamaX.Portal.Config.NHibernateFluentAutoMap;

/// <summary>
/// 
/// </summary>
public class PayamaxConfig : DefaultAutomappingConfiguration
{
    /// <summary>
    /// 
    /// </summary>
    /// <param name="type"></param>
    /// <returns></returns>
    public override bool ShouldMap(Type type)
    {
        ArgumentNullException.ThrowIfNull(type);
        return type.Name.EndsWith("Entity", StringComparison.InvariantCultureIgnoreCase);
    }

    /// <summary>
    /// 
    /// </summary>
    /// <param name="type"></param>
    /// <returns></returns>
    public override bool IsComponent(Type type)
    {
        ArgumentNullException.ThrowIfNull(type);
        return type.Name.EndsWith("Component", StringComparison.InvariantCultureIgnoreCase);
    }
    
}