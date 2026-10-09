import 'package:flutter/material.dart';
import 'package:thingsboard_app/config/themes/app_colors.dart';
import 'package:thingsboard_app/config/themes/tb_text_styles.dart';
import 'package:thingsboard_app/modules/main/model/main_navigation_item.dart';

class MoreMenuItemWidget extends StatelessWidget {
  const MoreMenuItemWidget(
    this.menuItem, {
    required this.onTap,
    this.color,
    this.showTrailing = true,
    this.subtitle,
    this.accent = false,
    super.key,
  });

  final TbMainNavigationItem menuItem;
  final VoidCallback onTap;
  final Color? color;
  final bool showTrailing;
  final String? subtitle;

  /// 渲染带白底卡片 + 色块图标的强调样式。
  /// 仅由「RTU 采集器」一类独立功能入口使用，
  /// 关闭时保持原有 ListTile 行为与视觉，不影响其它菜单项。
  final bool accent;

  static const double _radius = 12;
  static const double _iconTileSize = 36;

  @override
  Widget build(BuildContext context) {
    if (!accent) {
      return ListTile(
        onTap: onTap,
        horizontalTitleGap: 0,
        leading: Icon(
          menuItem.icon,
          color: color ?? AppColors.iconSecondary,
          size: 24,
        ),
        title: Text(
          menuItem.title,
          style: TbTextStyles.bodyMedium.copyWith(
            color: color ?? AppColors.textSecondary,
          ),
        ),
        trailing: showTrailing
            ? Icon(
                Icons.chevron_right,
                color: color ?? AppColors.iconDisabled,
              )
            : null,
      );
    }

    return _buildAccentTile();
  }

  Widget _buildAccentTile() {
    final accentColor = color ?? AppColors.primaryBlue;
    return Material(
      color: AppColors.white,
      borderRadius: BorderRadius.circular(_radius),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(_radius),
        overlayColor: WidgetStateProperty.all(AppColors.selectionColor),
        child: DecoratedBox(
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(_radius),
            border: Border.all(color: AppColors.bordersLight),
          ),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 11),
            child: Row(
              spacing: 12,
              children: [
                Container(
                  width: _iconTileSize,
                  height: _iconTileSize,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(10),
                    color: accentColor.withValues(alpha: .08),
                  ),
                  child: Icon(
                    menuItem.icon,
                    color: accentColor,
                    size: 18,
                  ),
                ),
                Expanded(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        menuItem.title,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TbTextStyles.labelMedium.copyWith(
                          color: AppColors.textSecondary,
                        ),
                      ),
                      if (subtitle != null)
                        Text(
                          subtitle!,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TbTextStyles.caption.copyWith(
                            color: AppColors.textDisabled,
                          ),
                        ),
                    ],
                  ),
                ),
                if (showTrailing)
                  const Icon(
                    Icons.chevron_right,
                    size: 18,
                    color: AppColors.iconDisabled,
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
