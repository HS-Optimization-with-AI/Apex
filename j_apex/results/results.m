close all;
clear all;

f2 = fopen('./test-results/0.12-0.08(1-4)/8.1', 'rt');

apex = zeros(3, 10, 3, 'double');
stub = zeros(3, 10, 3, 'double');

list = [2, 10, 100];
temp = '';

for i = 1:3
    size = list(i);
    num = 1000/size;
    
    f1 = fopen(strcat(strcat(strcat('./Apex-', num2str(size)), strcat('-', num2str(num))), '.txt'), 'rt');
    
    full_file = zeros(10, 3, 'double');
    
    for j = 1:10      
        while(contains(temp, "Total write") == 0)
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
        while(contains(temp, "Total write") == 0)
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


for i=1:3
    figure
    hold on
    bar([apex_fps(1, i), stub_fps(1, i)])
    errorbar([apex_fps(1, i), stub_fps(1, i)],[apex_err(1, i), stub_err(1, i)] ,'.')
    set(gca, 'xticklabel', {'Apex', 'Base'});
end



    