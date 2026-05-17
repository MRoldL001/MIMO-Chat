from PIL import Image
import os

# 定义各个 mipmap 尺寸和文件夹
icon_sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# 原始图标路径
original_icon_path = "D:/code/MIMOChat/mimo_chat.png"

# 确保原始图标存在
if not os.path.exists(original_icon_path):
    print(f"错误: 找不到原始图标文件 {original_icon_path}")
else:
    # 打开原始图片
    img = Image.open(original_icon_path)
    
    # 处理每个尺寸
    for folder, size in icon_sizes.items():
        # 确保文件夹存在
        folder_path = os.path.join("D:/code/MIMOChat/app/src/main/res", folder)
        if not os.path.exists(folder_path):
            os.makedirs(folder_path)
        
        # 调整大小 - 保持宽高比，同时缩小到指定尺寸内，增加适当的留白
        # 计算缩放比例，使图标占比约 70-75%，避免被过度裁剪
        target_size = int(size * 0.75)
        ratio = min(target_size / img.width, target_size / img.height)
        new_width = int(img.width * ratio)
        new_height = int(img.height * ratio)
        
        resized_img = img.resize((new_width, new_height), Image.Resampling.LANCZOS)
        
        # 创建新的背景画布
        new_img = Image.new("RGBA", (size, size), (255, 255, 255, 0))
        
        # 计算居中位置
        x = (size - new_width) // 2
        y = (size - new_height) // 2
        
        # 将调整后的图标粘贴到画布上
        new_img.paste(resized_img, (x, y), resized_img if resized_img.mode == 'RGBA' else None)
        
        # 保存为 ic_launcher.png
        save_path = os.path.join(folder_path, "ic_launcher.png")
        new_img.save(save_path, "PNG")
        print(f"已保存: {save_path}")
        
        # 保存为 ic_launcher_round.png
        save_path_round = os.path.join(folder_path, "ic_launcher_round.png")
        new_img.save(save_path_round, "PNG")
        print(f"已保存: {save_path_round}")

print("所有图标尺寸调整完成!")
