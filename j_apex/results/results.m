close all;
clear all;

apex = zeros(3, 10, 3, 'double');
stub = zeros(3, 10, 3, 'double');

list = [1, 2, 10];
names = {'Write', 'Read', 'Delete'};
temp = '';

for i = 1:3
    size = list(i);
    num = 1000/size;
    
    f1 = fopen(strcat(strcat(strcat('./Apex-', num2str(size)), strcat('-', num2str(num))), '.txt'), 'rt');
    
    full_file = zeros(10, 3, 'double');
    
    for j = 1:10      
        while(contains(temp, 'Total write') == 0)
            temp = fgetl(f1);
        end
        a = strsplit((temp), ' ');
%         disp(str2double(a(5)));
        full_file(j, 1) = str2double(a(5));
        temp = fgetl(f1);
        a = strsplit((temp), ' ');
        full_file(j, 2) = str2double(a(5));
        temp = fgetl(f1);
        a = strsplit((temp), ' ');
        full_file(j, 3) = str2double(a(5));
    end
    
%     disp(full_file);
    apex(i, :, :) = full_file(:, :);
end

for i = 1:3
    size = list(i);
    num = 1000/size;
    
    f1 = fopen(strcat(strcat(strcat('./Stub-', num2str(size)), strcat('-', num2str(num))), '.txt'), 'rt');
    
    full_file = zeros(10, 3, 'double');
    
    for j = 1:10      
        while(contains(temp, 'Total write') == 0)
            temp = fgetl(f1);
        end
        a = strsplit((temp), ' ');
%         disp(str2double(a(5)));
        full_file(j, 1) = str2double(a(5));
        temp = fgetl(f1);
        a = strsplit((temp), ' ');
        full_file(j, 2) = str2double(a(5));
        temp = fgetl(f1);
        a = strsplit((temp), ' ');
        full_file(j, 3) = str2double(a(5));
    end
    
%     disp(full_file);
    stub(i, :, :) = full_file(:, :);
end

apex_fps = zeros(3, 3);
stub_fps = zeros(3, 3);
apex_err = zeros(3, 3);
stub_err = zeros(3, 3);

for i=1:3
    for j=1:3
        apex_fps(i,j) = 1000*1000/(list(i) * mean(apex(i, :, j)));
        stub_fps(i,j) = 1000*1000/(list(i) * mean(stub(i, :, j)));
        apex_err(i,j) = 1000*1000*std(apex(i, :, j))/(list(i) * mean(apex(i, :, j)) * mean(apex(i, :, j)));
        stub_err(i,j) = 1000*1000*std(stub(i, :, j))/(list(i) * mean(stub(i, :, j)) * mean(stub(i, :, j)));
    end
end

disp(apex_fps(:,:));
disp(stub_fps(:,:));
disp(apex_err(:,:));
disp(stub_err(:,:));

for p=1:3
    figure
    for i=1:3
        s(i) = subplot(1, 3, i);
        ax = gca;
        ax.XTick = [1 2];
        ax.FontSize = 26;
        hold on
        b1 = bar([0, stub_fps(p, i)], 0.5, 'FaceColor', [0.502 0.502 0.502]);
        b2 = bar([apex_fps(p, i), 0], 0.5, 'FaceColor', [0.85 0.33 0.01]);
        errorbar([apex_fps(p, i), stub_fps(p, i)],[apex_err(p, i), stub_err(p, i)] ,'.', 'Color', [0 0 0], 'LineWidth', 1.5)
        set(gca, 'xticklabel', {'Apex', 'Base'});
        title(names(i))
    end
    ylabel(s(1), 'Files per second');
end


    